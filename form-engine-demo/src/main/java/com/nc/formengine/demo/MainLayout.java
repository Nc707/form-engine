package com.nc.formengine.demo;

import com.nc.formengine.flow.FormEngineViews;
import com.nc.formengine.flow.FormBuilderIndexView;
import com.nc.formengine.flow.FormIndexView;
import com.nc.formengine.flow.FormResponsesView;
import com.nc.formengine.flow.SubmissionListView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.Map;

/**
 * Application shell.
 *
 * <p>This is the other half of what a library cannot ship. A single unqualified {@code @Layout}
 * becomes the shell of every route in the application, so one arriving in a jar would give whoever
 * depended on it a drawer they did not ask for, and would collide with the layout they already
 * have. Left here, Vaadin wraps the library views in it anyway, and an application with its own
 * layout gets that one instead without configuring anything.
 *
 * <p>The drawer is built from {@link FormEngineViews#TOP_LEVEL}, with the titles and icons chosen
 * here rather than by the library.
 */
@Layout
public final class MainLayout extends AppLayout {

    private static final Map<Class<? extends Component>, VaadinIcon> ICONS = Map.of(
            FormBuilderIndexView.class, VaadinIcon.TOOLS,
            FormIndexView.class, VaadinIcon.RECORDS,
            SubmissionListView.class, VaadinIcon.INBOX,
            FormResponsesView.class, VaadinIcon.CLIPBOARD_TEXT);

    MainLayout() {
        setPrimarySection(Section.DRAWER);
        addToDrawer(createHeader(), new Scroller(createSideNav()));
    }

    private Component createHeader() {
        var appLogo = VaadinIcon.FORM.create();
        appLogo.setSize("40px");
        appLogo.setColor("var(--lumo-primary-color)");

        var appName = new Span("Form Engine");
        appName.getStyle().setFontWeight(Style.FontWeight.BOLD);

        var header = new VerticalLayout(appLogo, appName);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        return header;
    }

    private SideNav createSideNav() {
        var nav = new SideNav();
        nav.addClassNames(LumoUtility.Margin.Horizontal.MEDIUM);
        // By class, not by path: the paths are configuration, and SideNavItem resolves them from
        // wherever the views were mounted.
        FormEngineViews.TOP_LEVEL.forEach(view -> nav.addItem(new SideNavItem(
                view.suggestedTitle(),
                view.navigationTarget(),
                ICONS.get(view.navigationTarget()).create())));
        return nav;
    }
}
