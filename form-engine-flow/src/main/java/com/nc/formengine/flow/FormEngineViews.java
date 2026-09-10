package com.nc.formengine.flow;

import com.vaadin.flow.component.Component;

import java.util.List;

/**
 * The views worth offering from an application menu.
 *
 * <p>The module ships no {@code @Menu} annotations on purpose. They would fix the order, the title
 * and the icon inside the jar, where an application cannot reorder them against its own views,
 * cannot rename them and cannot translate them. What it gets instead is this list and public view
 * classes, which is enough to build a menu item and keep the presentation:
 *
 * <pre>{@code
 * FormEngineViews.TOP_LEVEL.forEach(view ->
 *         nav.addItem(new SideNavItem(view.suggestedTitle(), view.navigationTarget())));
 * }</pre>
 *
 * <p>Naming a view class directly works just as well, and is the better option for an application
 * that only wants some of them. Either way the URL is resolved from wherever the view was mounted,
 * so nothing here breaks when the paths are reconfigured.
 *
 * <p>The views that take an identifier are not listed: they are reached from these.
 */
public final class FormEngineViews {

    /** In a sensible reading order: author a form, fill one in, then look at what came back. */
    public static final List<Entry> TOP_LEVEL = List.of(
            new Entry(FormBuilderIndexView.class, "Form builder"),
            new Entry(FormIndexView.class, "Fill a form"),
            new Entry(SubmissionListView.class, "Responses"),
            new Entry(FormResponsesView.class, "Responses by form"));

    private FormEngineViews() {
    }

    /**
     * @param navigationTarget the view class, which is also what to navigate to
     * @param suggestedTitle   a reasonable English label, there to be replaced
     */
    public record Entry(Class<? extends Component> navigationTarget, String suggestedTitle) {
    }
}
