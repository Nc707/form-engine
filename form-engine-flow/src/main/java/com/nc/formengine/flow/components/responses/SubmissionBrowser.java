package com.nc.formengine.flow.components.responses;

import com.nc.formengine.business.service.FieldDefinitionService;
import com.nc.formengine.business.service.FormDefinitionService;
import com.nc.formengine.flow.utils.responses.AnswerResolver;
import com.nc.formengine.flow.utils.responses.SubmissionCsvWriter;
import com.nc.formengine.flow.view.responses.SubmissionDetailView;
import com.nc.formengine.model.dto.FieldDefinitionDTO;
import com.nc.formengine.model.dto.FormDefinitionDTO;
import com.nc.formengine.submission.business.service.FormSubmissionService;
import com.nc.formengine.submission.model.dto.FormSubmissionDTO;
import com.nc.formengine.submission.model.dto.SubmissionFilter;
import com.nc.formengine.submission.model.enums.SubmissionStatus;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.spring.data.VaadinSpringDataHelpers;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * The list of submissions, with the filters that narrow it and the export that follows them.
 *
 * <p>Shared by the two screens that show submissions — all of them, or one form's — because they
 * differ only in whether the form is chosen or given.
 */
public class SubmissionBrowser extends Composite<VerticalLayout> {

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * A page of an unordered query is not a stable page: without an order the database is free to
     * return rows differently between the two requests that fetch page 1 and page 2, so scrolling
     * would skip and repeat rows.
     */
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt")
            .and(Sort.by(Sort.Direction.DESC, "id"));

    private final FormSubmissionService submissionService;
    private final FieldDefinitionService fieldDefinitionService;
    private final AnswerResolver answerResolver;

    private final ComboBox<FormDefinitionDTO> formFilter = new ComboBox<>("Form");
    private final ComboBox<SubmissionStatus> statusFilter = new ComboBox<>("Status");
    private final TextField authorFilter = new TextField("Author");
    private final Grid<FormSubmissionDTO> grid = new Grid<>();
    private final Anchor exportLink;

    private final Map<Long, FormDefinitionDTO> formsById;

    /**
     * Read by the download handler, which runs in its own HTTP request with no lock on this UI —
     * so it must never reach into the filter components themselves.
     */
    private volatile SubmissionFilter currentFilter = new SubmissionFilter(null, null, null);

    private Consumer<SubmissionFilter> filterListener = filter -> {
    };

    public SubmissionBrowser(FormSubmissionService submissionService,
                      FormDefinitionService formDefinitionService,
                      FieldDefinitionService fieldDefinitionService,
                      AnswerResolver answerResolver) {
        this.submissionService = submissionService;
        this.fieldDefinitionService = fieldDefinitionService;
        this.answerResolver = answerResolver;

        this.formsById = formDefinitionService.findAll().stream()
                .collect(Collectors.toMap(FormDefinitionDTO::getId, form -> form,
                        (a, b) -> a, LinkedHashMap::new));

        this.exportLink = new Anchor(csvDownload(), "Export CSV");

        configureFilters();
        configureGrid();

        var layout = getContent();
        layout.setSizeFull();
        layout.setPadding(false);
        layout.add(filterBar(), grid);
        layout.setFlexGrow(1, grid);

        refresh();
    }

    // -- configuration -------------------------------------------------------

    private void configureFilters() {
        formFilter.setItems(formsById.values());
        formFilter.setItemLabelGenerator(SubmissionBrowser::describe);
        formFilter.setClearButtonVisible(true);
        formFilter.setPlaceholder("All");
        formFilter.addValueChangeListener(event -> refresh());

        statusFilter.setItems(SubmissionStatus.values());
        statusFilter.setClearButtonVisible(true);
        statusFilter.setPlaceholder("All");
        statusFilter.addValueChangeListener(event -> refresh());

        authorFilter.setPlaceholder("Search by author");
        authorFilter.setClearButtonVisible(true);
        authorFilter.setValueChangeMode(ValueChangeMode.LAZY);
        authorFilter.addValueChangeListener(event -> refresh());

        exportLink.getElement().setAttribute("theme", "button");
    }

    private HorizontalLayout filterBar() {
        var bar = new HorizontalLayout(formFilter, statusFilter, authorFilter, exportLink);
        bar.setWidthFull();
        bar.setWrap(true);
        bar.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.END);
        bar.addClassNames(LumoUtility.Padding.Horizontal.MEDIUM, LumoUtility.Padding.Vertical.SMALL);
        return bar;
    }

    private void configureGrid() {
        grid.setSizeFull();

        grid.addColumn(submission -> describe(formsById.get(submission.getFormDefinitionId())))
                .setHeader("Form")
                .setAutoWidth(true);

        grid.addColumn(FormSubmissionDTO::getAuthor)
                .setHeader("Author")
                .setSortProperty("author")
                .setAutoWidth(true);

        grid.addColumn(submission -> submission.getCreatedAt() == null
                        ? "—" : submission.getCreatedAt().format(TIMESTAMP))
                .setHeader("Started")
                .setSortProperty("createdAt")
                .setAutoWidth(true);

        grid.addColumn(SubmissionBrowser::describeSubmittedAt)
                .setHeader("Submitted")
                .setSortProperty("submittedAt")
                .setAutoWidth(true);

        grid.addComponentColumn(submission -> statusBadge(submission.getStatus()))
                .setHeader("Status")
                .setSortProperty("status")
                .setAutoWidth(true);

        grid.addItemClickListener(event -> getUI().ifPresent(ui -> ui.navigate(
                SubmissionDetailView.class,
                new RouteParameters(SubmissionDetailView.SUBMISSION_ID,
                        String.valueOf(event.getItem().getId())))));

        grid.setItems(
                query -> submissionService.findAll(currentFilter, toPageable(query)).stream(),
                query -> (int) totalMatching());
    }

    /**
     * The grid asks in offsets and limits, Spring Data answers in pages; the helper is what keeps
     * those two from being confused for each other.
     */
    private Pageable toPageable(Query<FormSubmissionDTO, Void> query) {
        var page = VaadinSpringDataHelpers.toSpringPageRequest(query);
        return page.getSort().isSorted()
                ? page
                : PageRequest.of(page.getPageNumber(), page.getPageSize(), DEFAULT_SORT);
    }

    private long totalMatching() {
        return submissionService.countByStatus(currentFilter).values().stream()
                .mapToLong(Long::longValue)
                .sum();
    }

    // -- state ---------------------------------------------------------------

    private void refresh() {
        FormDefinitionDTO form = formFilter.getValue();
        currentFilter = new SubmissionFilter(
                form == null ? null : form.getId(),
                statusFilter.getValue(),
                authorFilter.getValue());

        // Columns are one per field of one definition, so there is no honest set of them to export
        // while the list still spans several forms.
        exportLink.setEnabled(form != null);

        grid.getLazyDataView().refreshAll();
        filterListener.accept(currentFilter);
    }

    /** Pins the browser to one form and takes the form out of the filters. */
    public void lockToForm(FormDefinitionDTO form) {
        formFilter.setValue(form);
        formFilter.setVisible(false);
        refresh();
    }

    public void onFilterChange(Consumer<SubmissionFilter> listener) {
        this.filterListener = listener;
        listener.accept(currentFilter);
    }

    SubmissionFilter currentFilter() {
        return currentFilter;
    }

    // -- export --------------------------------------------------------------

    private DownloadHandler csvDownload() {
        return DownloadHandler.fromInputStream(event -> {
            SubmissionFilter filter = currentFilter;
            if (filter.formDefinitionId() == null) {
                return DownloadResponse.error(409);
            }

            byte[] csv = buildCsv(filter).getBytes(StandardCharsets.UTF_8);
            FormDefinitionDTO form = formsById.get(filter.formDefinitionId());
            String fileName = "responses-%s-v%s.csv".formatted(
                    form == null ? filter.formDefinitionId() : form.getCode(),
                    form == null ? "" : form.getVersion());

            return new DownloadResponse(new ByteArrayInputStream(csv), fileName,
                    "text/csv; charset=UTF-8", csv.length);
        });
    }

    private String buildCsv(SubmissionFilter filter) {
        List<FieldDefinitionDTO> fields =
                fieldDefinitionService.findByFormDefinitionId(filter.formDefinitionId());

        List<String> header =
                new ArrayList<>(List.of("Form", "Author", "Started", "Submitted", "Status"));
        fields.forEach(field -> header.add(
                field.getLabel() != null ? field.getLabel() : field.getName()));

        List<List<String>> rows = submissionService
                .findAll(filter, Pageable.unpaged(DEFAULT_SORT))
                .getContent().stream()
                .map(submission -> row(submission, fields))
                .toList();

        return SubmissionCsvWriter.write(header, rows);
    }

    private List<String> row(FormSubmissionDTO submission, List<FieldDefinitionDTO> fields) {
        List<String> cells = new ArrayList<>(List.of(
                submission.getFormCode() == null ? "" : submission.getFormCode(),
                submission.getAuthor() == null ? "" : submission.getAuthor(),
                submission.getCreatedAt() == null ? "" : submission.getCreatedAt().format(TIMESTAMP),
                describeSubmittedAt(submission),
                submission.getStatus() == null ? "" : submission.getStatus().name()));

        // The resolver emits the definition's own fields first, in their declared order, so dropping
        // the retired ones leaves exactly one cell per header column. Retired answers have no column
        // to go in: the header is fixed by the definition this export is for.
        answerResolver.resolve(submission, fields).stream()
                .filter(answer -> !answer.retired())
                .forEach(answer -> cells.add(answer.displayValue()));

        return cells;
    }

    // -- rendering -----------------------------------------------------------

    public static String describe(FormDefinitionDTO form) {
        if (form == null) {
            return "(definition deleted)";
        }
        return "%s (v%s · %s)".formatted(form.getTitle(), form.getVersion(), form.getStatus());
    }

    /** Null while it was never sent, which is what the column now stores rather than something else. */
    private static String describeSubmittedAt(FormSubmissionDTO submission) {
        return submission.getSubmittedAt() == null ? "—" : submission.getSubmittedAt().format(TIMESTAMP);
    }

    public static Span statusBadge(SubmissionStatus status) {
        var badge = new Span(status == null ? "—" : status.name());
        badge.getElement().setAttribute("theme", "badge " + switch (status == null ? SubmissionStatus.DRAFT : status) {
            case SUBMITTED -> "success";
            // Voiding a received response is a decision about it; abandoning a draft is not.
            case VOIDED -> "error";
            case DISCARDED, DRAFT -> "contrast";
        });
        return badge;
    }
}
