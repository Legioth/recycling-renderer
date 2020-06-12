package org.vaadin.leif;

import java.util.stream.IntStream;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route("")
@JsModule("./recyclingRenderer.js")
public class MainView extends VerticalLayout {
    public MainView() {
        Grid<String> grid = new Grid<>();
        grid.setItems(IntStream.range(0, 1000).mapToObj(number -> "Item " + number));

        grid.addColumn(RecyclingRenderer.withoutReset(Div::new, Div::setText)).setHeader("Without reset");

        grid.addColumn(RecyclingRenderer.withReset(Div::new, (component, item) -> {
            component.setText(item);
            return () -> component.setText("Cleared from server");
        })).setHeader("With reset");

        add(new HorizontalLayout(new Button("Set height 2000px", event -> grid.setHeight("2000px")),
                new Button("Set height null", event -> grid.setHeight(null))), grid);
    }
}
