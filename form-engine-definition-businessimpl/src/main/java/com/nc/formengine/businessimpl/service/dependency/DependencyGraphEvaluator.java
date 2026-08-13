package com.nc.formengine.businessimpl.service.dependency;

import com.nc.formengine.model.dependency.FieldState;
import com.nc.formengine.model.dto.FieldDefinitionDTO;
import com.nc.formengine.model.dto.FieldDependencyDTO;
import com.nc.formengine.model.enums.DependencyEffect;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resolves a form's conditional dependencies into a state per field.
 * <p>
 * Pure: it takes plain lists in and gives a plain map back, with no persistence and no container, so
 * the whole rule set is unit testable in milliseconds. {@code DependencyEvaluationServiceImpl} only
 * loads the data and delegates here.
 *
 * <h2>How it resolves</h2>
 * A dependency reads "when the trigger field's value satisfies the condition, apply the effect to the
 * dependent field", which makes the form a directed graph of trigger &rarr; dependent edges. Fields
 * are resolved in dependency order, so a chain A &rarr; B &rarr; C resolves in a single pass.
 * <p>
 * Ordering comes from a Tarjan strongly-connected-components pass, which also handles cycles with a
 * single rule: <b>a dependency is ignored exactly when its trigger and its dependent field belong to
 * the same component</b>. Self-references, two-field cycles and longer ones all collapse into that
 * one case, while dependencies entering a cycle from outside — and every field downstream of it —
 * keep working normally. Tarjan emits components in reverse topological order, so walking its output
 * backwards visits every field after the fields it depends on. The whole thing is O(V+E) and cannot
 * loop forever.
 *
 * <h2>The rules</h2>
 * Base state is visible, and required as declared by the field definition. Then, per field:
 * <ul>
 *   <li><b>SHOW gate</b> — a field targeted by at least one surviving {@code SHOW} dependency starts
 *       hidden and is visible only while one of them is satisfied. Fields with no {@code SHOW}
 *       dependency stay visible by default. The gate exists because {@code FieldDefinition} has no
 *       {@code visible} column: without it, {@code SHOW} could never change anything.</li>
 *   <li><b>Conflicts: the most restrictive effect wins</b> — {@code HIDE} beats {@code SHOW},
 *       {@code REQUIRE} beats {@code OPTIONAL}. The satisfied effects are accumulated into four flags
 *       and combined once, so the result is independent of the order the dependencies arrive in —
 *       which matters, because the database guarantees none.</li>
 *   <li><b>A hidden field has no value</b>, so every dependency it triggers is unsatisfied. This is
 *       what gives chains their meaning: hiding A neutralises the rules A drives.</li>
 * </ul>
 * There is deliberately no gate for {@code REQUIRE}: it would turn a field declared {@code required}
 * into an optional one merely because it carries a {@code REQUIRE} dependency, which fails open and
 * contradicts the conflict rule. {@code OPTIONAL} being a no-op on an already-optional field is
 * harmless.
 */
public final class DependencyGraphEvaluator {

    private DependencyGraphEvaluator() {
    }

    /**
     * Evaluates a form.
     *
     * @param fields       the form's fields; those without an id are skipped, as they cannot be
     *                     referenced by a dependency
     * @param dependencies the form's dependencies; rows that cannot be applied are skipped
     * @param values       the current answers, keyed by field name
     * @return the state of every usable field, keyed by name, plus any cycles found
     */
    public static EvaluationResult evaluate(List<FieldDefinitionDTO> fields,
                                            List<FieldDependencyDTO> dependencies,
                                            Map<String, Object> values) {

        Map<Long, FieldDefinitionDTO> fieldsById = indexById(fields);
        Map<String, Object> answers = values == null ? Map.of() : values;

        List<Long> nodes = new ArrayList<>(fieldsById.keySet());
        Map<Long, Integer> positions = new HashMap<>();
        for (int position = 0; position < nodes.size(); position++) {
            positions.put(nodes.get(position), position);
        }

        // Inbound dependencies drive the effects; the deduplicated edges drive the ordering. Repeated
        // (trigger, dependent) rows are legal and must not be counted twice by the graph pass.
        Map<Long, List<FieldDependencyDTO>> inbound = new HashMap<>();
        List<Set<Integer>> edges = new ArrayList<>(nodes.size());
        for (int i = 0; i < nodes.size(); i++) {
            edges.add(new LinkedHashSet<>());
        }
        if (dependencies != null) {
            for (FieldDependencyDTO dependency : dependencies) {
                if (!isApplicable(dependency, fieldsById)) {
                    continue;
                }
                inbound.computeIfAbsent(dependency.getDependentFieldId(), key -> new ArrayList<>())
                        .add(dependency);
                edges.get(positions.get(dependency.getTriggerFieldId()))
                        .add(positions.get(dependency.getDependentFieldId()));
            }
        }

        Components components = findComponents(edges);

        Map<Long, FieldState> statesById = new HashMap<>();
        // Tarjan emits components in reverse topological order, so walking backwards resolves every
        // field after the fields that drive it.
        for (int component = components.order().size() - 1; component >= 0; component--) {
            for (int position : components.order().get(component)) {
                Long fieldId = nodes.get(position);
                statesById.put(fieldId, resolve(
                        fieldsById.get(fieldId),
                        inbound.getOrDefault(fieldId, List.of()),
                        components, positions, fieldsById, statesById, answers));
            }
        }

        return new EvaluationResult(
                projectByName(fieldsById.values(), statesById),
                describeCycles(components, nodes, fieldsById, edges));
    }

    /** Resolves one field from its inbound dependencies and the already-resolved fields. */
    private static FieldState resolve(FieldDefinitionDTO field,
                                      List<FieldDependencyDTO> inbound,
                                      Components components,
                                      Map<Long, Integer> positions,
                                      Map<Long, FieldDefinitionDTO> fieldsById,
                                      Map<Long, FieldState> statesById,
                                      Map<String, Object> answers) {

        int ownComponent = components.of(positions.get(field.getId()));

        boolean gated = false;
        boolean showSatisfied = false;
        boolean hideSatisfied = false;
        boolean requireSatisfied = false;
        boolean optionalSatisfied = false;

        for (FieldDependencyDTO dependency : inbound) {
            // The cycle rule: a dependency inside a component does not exist as far as this pass is
            // concerned. The SHOW gate is computed from the survivors only, otherwise a field caught
            // in a cycle would stay gated with nothing left able to open it.
            if (components.of(positions.get(dependency.getTriggerFieldId())) == ownComponent) {
                continue;
            }
            if (dependency.getEffect() == DependencyEffect.SHOW) {
                gated = true;
            }
            if (!holds(dependency, fieldsById, statesById, answers)) {
                continue;
            }
            switch (dependency.getEffect()) {
                case SHOW -> showSatisfied = true;
                case HIDE -> hideSatisfied = true;
                case REQUIRE -> requireSatisfied = true;
                case OPTIONAL -> optionalSatisfied = true;
            }
        }

        boolean visible = (!gated || showSatisfied) && !hideSatisfied;

        // required is a nullable Boolean on the DTO and null in the database whenever a field was
        // saved through the mapper rather than the builder, so unboxing it directly would throw.
        boolean required = Boolean.TRUE.equals(field.getRequired());
        if (optionalSatisfied) {
            required = false;
        }
        if (requireSatisfied) {
            required = true;
        }

        return new FieldState(visible, required);
    }

    /** Whether a dependency's condition holds against the current answer for its trigger field. */
    private static boolean holds(FieldDependencyDTO dependency,
                                 Map<Long, FieldDefinitionDTO> fieldsById,
                                 Map<Long, FieldState> statesById,
                                 Map<String, Object> answers) {

        FieldState triggerState = statesById.get(dependency.getTriggerFieldId());
        if (triggerState == null || !triggerState.visible()) {
            return false;
        }
        String triggerName = fieldsById.get(dependency.getTriggerFieldId()).getName();
        if (triggerName == null) {
            return false;
        }
        return ConditionEvaluator.matches(
                dependency.getCondition(), answers.get(triggerName), dependency.getTriggerValue());
    }

    /**
     * A dependency is applicable when both of its endpoints are fields of this form and it says both
     * what to test and what to do. Anything else is a broken row that cannot be acted on.
     */
    private static boolean isApplicable(FieldDependencyDTO dependency,
                                        Map<Long, FieldDefinitionDTO> fieldsById) {
        return dependency != null
                && dependency.getCondition() != null
                && dependency.getEffect() != null
                && fieldsById.containsKey(dependency.getDependentFieldId())
                && fieldsById.containsKey(dependency.getTriggerFieldId());
    }

    private static Map<Long, FieldDefinitionDTO> indexById(List<FieldDefinitionDTO> fields) {
        Map<Long, FieldDefinitionDTO> fieldsById = new LinkedHashMap<>();
        if (fields != null) {
            for (FieldDefinitionDTO field : fields) {
                if (field != null && field.getId() != null) {
                    fieldsById.put(field.getId(), field);
                }
            }
        }
        return fieldsById;
    }

    /**
     * Keys the result by field name, as the service contract requires.
     *
     * <p>A name identifies a field within its form — enforced by the field service and by a unique
     * constraint on {@code (form_definition_id, name)} — so no state can be lost to a collision here.
     * It once could: two fields of one name meant the later one silently replaced the earlier, dropping
     * it from both dependency resolution and validation with nothing anywhere to say why.
     */
    private static Map<String, FieldState> projectByName(Iterable<FieldDefinitionDTO> fields,
                                                         Map<Long, FieldState> statesById) {
        Map<String, FieldState> byName = new LinkedHashMap<>();
        for (FieldDefinitionDTO field : fields) {
            if (field.getName() != null) {
                byName.put(field.getName(), statesById.get(field.getId()));
            }
        }
        return byName;
    }

    private static List<List<String>> describeCycles(Components components,
                                                     List<Long> nodes,
                                                     Map<Long, FieldDefinitionDTO> fieldsById,
                                                     List<Set<Integer>> edges) {
        List<List<String>> cycles = new ArrayList<>();
        for (List<Integer> component : components.order()) {
            boolean cyclic = component.size() > 1
                    || edges.get(component.get(0)).contains(component.get(0));
            if (!cyclic) {
                continue;
            }
            List<String> names = new ArrayList<>(component.size());
            for (int position : component) {
                Long fieldId = nodes.get(position);
                String name = fieldsById.get(fieldId).getName();
                names.add(name != null ? name : "#" + fieldId);
            }
            cycles.add(List.copyOf(names));
        }
        return List.copyOf(cycles);
    }

    /**
     * Tarjan's strongly connected components, iterative so that a long dependency chain cannot
     * overflow the stack.
     *
     * @param edges adjacency by node position
     * @return the component of each node, and the components themselves in the order Tarjan closed
     *         them, which is reverse topological
     */
    private static Components findComponents(List<Set<Integer>> edges) {
        int size = edges.size();
        int[] discovered = new int[size];
        int[] lowLink = new int[size];
        int[] componentOf = new int[size];
        boolean[] onStack = new boolean[size];
        Arrays.fill(discovered, -1);
        Arrays.fill(componentOf, -1);

        List<List<Integer>> components = new ArrayList<>();
        Deque<Integer> pending = new ArrayDeque<>();
        // An explicit call stack: at most one frame per node.
        int[] frameNode = new int[size];
        int[] frameNextChild = new int[size];
        List<List<Integer>> adjacency = new ArrayList<>(size);
        for (Set<Integer> targets : edges) {
            adjacency.add(List.copyOf(targets));
        }

        int counter = 0;
        for (int start = 0; start < size; start++) {
            if (discovered[start] != -1) {
                continue;
            }
            int top = 0;
            frameNode[0] = start;
            frameNextChild[0] = 0;
            discovered[start] = counter;
            lowLink[start] = counter;
            counter++;
            pending.push(start);
            onStack[start] = true;

            while (top >= 0) {
                int node = frameNode[top];
                List<Integer> children = adjacency.get(node);

                if (frameNextChild[top] < children.size()) {
                    int child = children.get(frameNextChild[top]++);
                    if (discovered[child] == -1) {
                        discovered[child] = counter;
                        lowLink[child] = counter;
                        counter++;
                        pending.push(child);
                        onStack[child] = true;
                        top++;
                        frameNode[top] = child;
                        frameNextChild[top] = 0;
                    } else if (onStack[child]) {
                        lowLink[node] = Math.min(lowLink[node], discovered[child]);
                    }
                    continue;
                }

                if (lowLink[node] == discovered[node]) {
                    List<Integer> component = new ArrayList<>();
                    int member;
                    do {
                        member = pending.pop();
                        onStack[member] = false;
                        componentOf[member] = components.size();
                        component.add(member);
                    } while (member != node);
                    components.add(List.copyOf(component));
                }

                top--;
                if (top >= 0) {
                    lowLink[frameNode[top]] = Math.min(lowLink[frameNode[top]], lowLink[node]);
                }
            }
        }

        return new Components(componentOf, List.copyOf(components));
    }

    /** The component decomposition of the dependency graph. */
    private record Components(int[] componentOf, List<List<Integer>> order) {

        int of(int position) {
            return componentOf[position];
        }
    }
}
