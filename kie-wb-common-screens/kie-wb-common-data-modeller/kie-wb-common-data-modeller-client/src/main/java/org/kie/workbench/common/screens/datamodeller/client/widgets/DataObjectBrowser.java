/**
 * Copyright 2012 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.workbench.common.screens.datamodeller.client.widgets;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.CellTable;
import com.github.gwtbootstrap.client.ui.TooltipCellDecorator;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.*;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.kie.workbench.common.screens.datamodeller.client.DataModelerContext;
import org.kie.workbench.common.screens.datamodeller.client.resources.i18n.Constants;
import org.kie.workbench.common.screens.datamodeller.client.resources.images.ImagesResources;
import org.kie.workbench.common.screens.datamodeller.client.util.AnnotationValueHandler;
import org.kie.workbench.common.screens.datamodeller.client.util.DataModelerUtils;
import org.kie.workbench.common.screens.datamodeller.client.util.ObjectPropertyComparator;
import org.kie.workbench.common.screens.datamodeller.client.validation.ValidatorCallback;
import org.kie.workbench.common.screens.datamodeller.client.validation.ValidatorService;
import org.kie.workbench.common.screens.datamodeller.events.*;
import org.kie.workbench.common.screens.datamodeller.model.AnnotationDefinitionTO;
import org.kie.workbench.common.screens.datamodeller.model.DataModelTO;
import org.kie.workbench.common.screens.datamodeller.model.DataObjectTO;
import org.kie.workbench.common.screens.datamodeller.model.ObjectPropertyTO;
import org.uberfire.client.common.ErrorPopup;

import javax.annotation.PostConstruct;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.*;


public class DataObjectBrowser extends Composite {

    interface DataObjectEditorUIBinder
            extends UiBinder<Widget, DataObjectBrowser> {

    };

    public static final String NOT_SELECTED = "NOT_SELECTED";

    private static DataObjectEditorUIBinder uiBinder = GWT.create(DataObjectEditorUIBinder.class);

    @UiField
    VerticalPanel mainPanel;

    @UiField
    SimplePanel breadCrumbsPanel;

    @Inject
    DataObjectBreadcrums dataObjectNavigation;

    @UiField
    Label objectName;

    @UiField(provided = true)
    CellTable<ObjectPropertyTO> dataObjectPropertiesTable = new CellTable<ObjectPropertyTO>(1000, GWT.<CellTable.SelectableResources>create(CellTable.SelectableResources.class));

    @UiField
    Label newPropertyHeader;

    @UiField
    Label newPropertyIdLabel;

    @UiField
    com.github.gwtbootstrap.client.ui.TextBox newPropertyId;

    @UiField
    Label newPropertyLabelLabel;

    @UiField
    com.github.gwtbootstrap.client.ui.TextBox newPropertyLabel;

    @UiField
    Label newPropertyTypeLabel;

    @UiField
    com.github.gwtbootstrap.client.ui.ListBox newPropertyType;

    @UiField
    Button newPropertyButton;

    private DataObjectTO dataObject;

    private DataModelerContext context;

    private ListDataProvider<ObjectPropertyTO> dataObjectPropertiesProvider = new ListDataProvider<ObjectPropertyTO>();

    private List<ObjectPropertyTO> dataObjectProperties = new ArrayList<ObjectPropertyTO>();

    @Inject
    private ValidatorService validatorService;

    @Inject
    Event<DataModelerEvent> dataModelerEvent;

    private boolean newPropertyActionEnabled = false;

    public DataObjectBrowser() {
        initWidget(uiBinder.createAndBindUi(this));

        newPropertyId.getElement().getStyle().setWidth(180, Style.Unit.PX);
        newPropertyLabel.getElement().getStyle().setWidth(160, Style.Unit.PX);
        newPropertyType.getElement().getStyle().setWidth(200, Style.Unit.PX);

        objectName.setText("");

        dataObjectPropertiesProvider.setList(dataObjectProperties);

        //Init data objects table

        dataObjectPropertiesTable.setEmptyTableWidget(new com.github.gwtbootstrap.client.ui.Label(Constants.INSTANCE.objectBrowser_emptyTable()));

        //Init delete column
        ClickableImageResourceCell clickableImageResourceCell = new ClickableImageResourceCell(true);
        final TooltipCellDecorator<ImageResource> decorator = new TooltipCellDecorator<ImageResource>(clickableImageResourceCell);
        decorator.setText(Constants.INSTANCE.objectBrowser_action_deleteProperty());

        final Column<ObjectPropertyTO, ImageResource> deletePropertyColumnImg = new Column<ObjectPropertyTO, ImageResource>(decorator) {
            @Override
            public ImageResource getValue( final ObjectPropertyTO global ) {
                return ImagesResources.INSTANCE.Delete();
            }
        };

        deletePropertyColumnImg.setFieldUpdater( new FieldUpdater<ObjectPropertyTO, ImageResource>() {
            public void update( final int index,
                                final ObjectPropertyTO property,
                                final ImageResource value ) {

                deleteDataObjectProperty(property, index);
            }
        } );


        dataObjectPropertiesTable.addColumn(deletePropertyColumnImg);
        dataObjectPropertiesTable.setColumnWidth(deletePropertyColumnImg, 20, Style.Unit.PX);


        //Init property name column

        final TextColumn<ObjectPropertyTO> propertyNameColumn = new TextColumn<ObjectPropertyTO>() {

            @Override
            public void render(Cell.Context context, ObjectPropertyTO object, SafeHtmlBuilder sb) {
                SafeHtml startDiv = new SafeHtml() {
                    @Override
                    public String asString() {
                        return "<div style=\"cursor: pointer;\">";
                    }
                };
                SafeHtml endDiv = new SafeHtml() {
                    @Override
                    public String asString() {
                        return "</div>";
                    }
                };

                sb.append(startDiv);
                super.render(context, object, sb);
                sb.append(endDiv);
            }

            @Override
            public String getValue( final ObjectPropertyTO objectProperty) {
                return objectProperty.getName();
            }
        };


        propertyNameColumn.setSortable(true);
        dataObjectPropertiesTable.addColumn(propertyNameColumn, Constants.INSTANCE.objectBrowser_columnName());
        //dataObjectPropertiesTable.setColumnWidth(propertyNameColumn, 100, Style.Unit.PX);


        ColumnSortEvent.ListHandler<ObjectPropertyTO> propertyNameColHandler = new ColumnSortEvent.ListHandler<ObjectPropertyTO>(dataObjectPropertiesProvider.getList());
        propertyNameColHandler.setComparator(propertyNameColumn, new ObjectPropertyComparator("name"));
        dataObjectPropertiesTable.addColumnSortHandler(propertyNameColHandler);


        //Init property Label column

        final TextColumn<ObjectPropertyTO> propertyLabelColumn = new TextColumn<ObjectPropertyTO>() {

            @Override
            public void render(Cell.Context context, ObjectPropertyTO object, SafeHtmlBuilder sb) {
                SafeHtml startDiv = new SafeHtml() {
                    @Override
                    public String asString() {
                        return "<div style=\"cursor: pointer;\">";
                    }
                };
                SafeHtml endDiv = new SafeHtml() {
                    @Override
                    public String asString() {
                        return "</div>";
                    }
                };

                sb.append(startDiv);
                super.render(context, object, sb);
                sb.append(endDiv);
            }

            @Override
            public String getValue( final ObjectPropertyTO objectProperty) {
                return AnnotationValueHandler.getInstance().getStringValue(objectProperty, AnnotationDefinitionTO.LABEL_ANNOTATION, AnnotationDefinitionTO.VALUE_PARAM);
            }
        };

        propertyLabelColumn.setSortable(true);
        dataObjectPropertiesTable.addColumn(propertyLabelColumn, Constants.INSTANCE.objectBrowser_columnLabel());
        //dataObjectPropertiesTable.setColumnWidth(propertyNameColumn, 100, Style.Unit.PX);

        ColumnSortEvent.ListHandler<ObjectPropertyTO> propertyLabelColHandler = new ColumnSortEvent.ListHandler<ObjectPropertyTO>(dataObjectPropertiesProvider.getList());
        propertyNameColHandler.setComparator(propertyLabelColumn, new ObjectPropertyComparator("label"));
        dataObjectPropertiesTable.addColumnSortHandler(propertyLabelColHandler);


        //Init property type browsing column
        ClickableImageResourceCell typeImageCell = new ClickableImageResourceCell(true);
        final TooltipCellDecorator<ImageResource> typeImageDecorator = new TooltipCellDecorator<ImageResource>(typeImageCell);
        typeImageDecorator.setText("Go to data object definition");

        final Column<ObjectPropertyTO, ImageResource> typeImageColumn = new Column<ObjectPropertyTO, ImageResource>(typeImageDecorator) {
            @Override
            public ImageResource getValue( final ObjectPropertyTO property ) {

                if (!property.isBaseType() && !getDataObject().getClassName().equals(property.getClassName()) && !getDataModel().isExternal(property.getClassName())) {
                    return ImagesResources.INSTANCE.BrowseObject();
                } else {
                    return null;
                }
            }
        };

        typeImageColumn.setFieldUpdater( new FieldUpdater<ObjectPropertyTO, ImageResource>() {
            public void update( final int index,
                                final ObjectPropertyTO property,
                                final ImageResource value ) {

                onTypeCellSelection(property);
            }
        } );

        dataObjectPropertiesTable.addColumn(typeImageColumn);
        dataObjectPropertiesTable.setColumnWidth(typeImageColumn, 20, Style.Unit.PX);


        //Init property type column
        final TextColumn<ObjectPropertyTO> propertyTypeColumn = new TextColumn<ObjectPropertyTO>() {

            @Override
            public void render(Cell.Context context, ObjectPropertyTO object, SafeHtmlBuilder sb) {
                SafeHtml startDiv = new SafeHtml() {
                    @Override
                    public String asString() {
                        return "<div style=\"cursor: pointer;\">";
                    }
                };
                SafeHtml endDiv = new SafeHtml() {
                    @Override
                    public String asString() {
                        return "</div>";
                    }
                };

                sb.append(startDiv);
                super.render(context, object, sb);
                sb.append(endDiv);
            }

            @Override
            public String getValue( final ObjectPropertyTO objectProperty) {
                return propertyTypeDisplay(objectProperty);
            }
        };
        propertyTypeColumn.setSortable(true);
        dataObjectPropertiesTable.addColumn(propertyTypeColumn, Constants.INSTANCE.objectBrowser_columnType());
        //dataObjectPropertiesTable.setColumnWidth(propertyTypeColumn, 100, Style.Unit.PX);


        ColumnSortEvent.ListHandler<ObjectPropertyTO> propertyTypeColHandler = new ColumnSortEvent.ListHandler<ObjectPropertyTO>(dataObjectPropertiesProvider.getList());
        propertyTypeColHandler.setComparator(propertyTypeColumn, new ObjectPropertyComparator("className"));
        dataObjectPropertiesTable.addColumnSortHandler(propertyTypeColHandler);


        //dataObjectPropertiesTable.getColumnSortList().push(propertyTypeColumn);
        dataObjectPropertiesTable.getColumnSortList().push(propertyNameColumn);

        //Init the selection model
        SingleSelectionModel<ObjectPropertyTO> selectionModel = new SingleSelectionModel<ObjectPropertyTO>();
        dataObjectPropertiesTable.setSelectionModel(selectionModel);
        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {

            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                ObjectPropertyTO selectedPropertyTO = ((SingleSelectionModel<ObjectPropertyTO>)dataObjectPropertiesTable.getSelectionModel()).getSelectedObject();
                notifyFieldSelected(selectedPropertyTO);
            }
        });

        dataObjectPropertiesTable.setKeyboardSelectionPolicy(HasKeyboardSelectionPolicy.KeyboardSelectionPolicy.BOUND_TO_SELECTION);

        dataObjectPropertiesProvider.addDataDisplay(dataObjectPropertiesTable);
        dataObjectPropertiesProvider.refresh();

        newPropertyButton.setIcon(IconType.PLUS_SIGN);

        enableNewPropertyAction(false);
    }
    
    @PostConstruct
    void completeUI() {
        dataObjectNavigation.setDivider(">");
        breadCrumbsPanel.add(dataObjectNavigation);
    }

    public DataModelerContext getContext() {
        return context;
    }

    public void setContext(DataModelerContext context) {
        this.context = context;
        dataObjectNavigation.setContext(context);
    }

    private void initTypeList() {
        newPropertyType.clear();
        newPropertyType.addItem("", NOT_SELECTED);

        SortedMap<String, String> typeNames = new TreeMap<String, String>();
        if (getDataModel() != null) {
            // First add all base types, ordered
            for (Map.Entry<String, String> baseType : getContext().getHelper().getOrderedBaseTypes().entrySet()) {
                newPropertyType.addItem(baseType.getKey(), baseType.getValue());
                // TODO add multiple types for base types?
            }

            // Second add all model types, ordered
            for (DataObjectTO dataObject : getDataModel().getDataObjects()) {
                String className = dataObject.getClassName();
                String className_m = className + DataModelerUtils.MULTIPLE;
                String classLabel = DataModelerUtils.getDataObjectFullLabel(dataObject);
                String classLabel_m = classLabel  + DataModelerUtils.MULTIPLE;
                typeNames.put(classLabel, className);
                typeNames.put(classLabel_m, className_m);
            }
            for (Map.Entry<String, String> typeName : typeNames.entrySet()) {
                newPropertyType.addItem(typeName.getKey(), typeName.getValue());
            }

            // Then add all external types, ordered
            typeNames.clear();
            for (String extClass : getDataModel().getExternalClasses()) {
                String extClass_m = extClass + DataModelerUtils.MULTIPLE;
                typeNames.put(DataModelerUtils.EXTERNAL_PREFIX + extClass, extClass);
                typeNames.put(DataModelerUtils.EXTERNAL_PREFIX + extClass_m, extClass_m);
            }
            for (Map.Entry<String, String> typeName : typeNames.entrySet()) {
                newPropertyType.addItem(typeName.getKey(), typeName.getValue());
            }
        }
    }

    private void createNewProperty(final DataObjectTO dataObject, final String propertyName, final String propertyLabel, final String propertyType) {
        if (dataObject != null) {
            validatorService.isValidIdentifier(propertyName, new ValidatorCallback() {
                @Override
                public void onFailure() {
                    ErrorPopup.showMessage(Constants.INSTANCE.validation_error_invalid_object_attribute_identifier(propertyName));
                }

                @Override
                public void onSuccess() {
                    validatorService.isUniqueAttributeName(propertyName, dataObject, new ValidatorCallback() {
                        @Override
                        public void onFailure() {
                            ErrorPopup.showMessage(Constants.INSTANCE.validation_error_object_attribute_already_exists(propertyName));
                        }

                        @Override
                        public void onSuccess() {
                            if (propertyType != null && !"".equals(propertyType) && !NOT_SELECTED.equals(propertyType)) {
                                Boolean isMultiple = DataModelerUtils.isMultipleType(propertyType);
                                String canonicalType = isMultiple ? DataModelerUtils.getCanonicalClassName(propertyType) : propertyType;
                                ObjectPropertyTO property = new ObjectPropertyTO(propertyName,
                                                                                 canonicalType,
                                                                                 isMultiple,
                                                                                 getContext().getHelper().isBaseType(canonicalType));
                                if (propertyLabel != null && !"".equals(propertyLabel)) {
                                    property.addAnnotation( getContext().getAnnotationDefinitions().get(AnnotationDefinitionTO.LABEL_ANNOTATION), AnnotationDefinitionTO.VALUE_PARAM, propertyLabel );
                                }
                                addDataObjectProperty(property);
                                resetInput();
                            } else {
                                ErrorPopup.showMessage(Constants.INSTANCE.validation_error_missing_object_attribute_type());
                            }
                        }
                    });
                }
            });
        }
    }
    
    private void setDataObject(DataObjectTO dataObject) {
        this.dataObject = dataObject;
        objectName.setText(DataModelerUtils.getDataObjectFullLabel(getDataObject()));

        //We create a new selection model due to a bug found in GWT when we change e.g. from one data object with 9 rows
        // to one with 3 rows and the table was sorted.
        //Several tests has been done and the final workaround (not too bad) we found is to
        // 1) sort the table again
        // 2) create a new selection model
        // 3) populate the table with new items
        // 3) select the first row
        SingleSelectionModel selectionModel2 = new SingleSelectionModel<ObjectPropertyTO>();
        dataObjectPropertiesTable.setSelectionModel(selectionModel2);

        selectionModel2.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {

            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                ObjectPropertyTO selectedPropertyTO = ((SingleSelectionModel<ObjectPropertyTO>)dataObjectPropertiesTable.getSelectionModel()).getSelectedObject();
                notifyFieldSelected(selectedPropertyTO);
            }
        });

        ArrayList<ObjectPropertyTO> sortBuffer = new ArrayList<ObjectPropertyTO>();
        if (dataObject != null) sortBuffer.addAll(dataObject.getProperties());
        Collections.sort(sortBuffer, new ObjectPropertyComparator("name"));

        dataObjectProperties = (dataObject != null) ? dataObject.getProperties() : Collections.<ObjectPropertyTO>emptyList();
        dataObjectPropertiesProvider.getList().clear();
        dataObjectPropertiesProvider.getList().addAll(sortBuffer);
        dataObjectPropertiesProvider.flush();
        dataObjectPropertiesProvider.refresh();

        dataObjectPropertiesTable.getColumnSortList().push(new ColumnSortList.ColumnSortInfo(dataObjectPropertiesTable.getColumn(1), true));

        if (dataObjectProperties.size() > 0) {
            dataObjectPropertiesTable.setKeyboardSelectedRow(0);
            selectionModel2.setSelected(sortBuffer.get(0), true);
        }

        //set the first row selected again. Sounds crazy, but's part of the workaround, don't remove this line.
        if (dataObjectProperties.size() > 0) {
            dataObjectPropertiesTable.setKeyboardSelectedRow(0);
        }
    }

    private void addDataObjectProperty(ObjectPropertyTO objectProperty) {
        if (dataObject != null) {
            dataObject.getProperties().add(objectProperty);

            if (!objectProperty.isBaseType()) getContext().getHelper().dataObjectReferenced(objectProperty.getClassName(), dataObject.getClassName());

            dataObjectPropertiesProvider.getList().add(objectProperty);
            dataObjectPropertiesProvider.flush();
            dataObjectPropertiesProvider.refresh();
            dataObjectPropertiesTable.setKeyboardSelectedRow(dataObjectPropertiesProvider.getList().size() - 1);
            notifyFieldCreated(objectProperty);
        }
    }

    private void deleteDataObjectProperty(ObjectPropertyTO objectProperty, int index) {
        if (dataObject != null) {
            dataObject.getProperties().remove(objectProperty);

            getContext().getHelper().dataObjectUnReferenced(objectProperty.getClassName(), dataObject.getClassName());

            dataObjectPropertiesProvider.getList().remove(index);
            dataObjectPropertiesProvider.flush();
            dataObjectPropertiesProvider.refresh();
            notifyFieldDeleted(objectProperty);
        }
    }

    private String propertyTypeDisplay(ObjectPropertyTO property) {
        String displayName = property.getClassName();
        if (property.isBaseType()) return DataModelerUtils.getInstance().extractClassName(displayName);
        String label = getContext().getHelper().getObjectLabelByClassName(displayName);
        if (label != null && !"".equals(label)) displayName = label;
        if (property.isMultiple()) {
            displayName += DataModelerUtils.MULTIPLE;
        }
        return displayName;
    }

    private DataModelTO getDataModel() {
        return getContext() != null ? getContext().getDataModel() : null;
    }

    public DataObjectTO getDataObject() {
        return dataObject;
    }

    public void onTypeCellSelection(ObjectPropertyTO property) {

        DataObjectTO dataObject = getDataModel().getDataObjectByClassName(property.getClassName());
        if (dataObject != null) {
            notifyObjectSelected(dataObject);
        }
    }

    private void enableNewPropertyAction(boolean enable) {
        newPropertyId.setEnabled(enable);
        newPropertyLabel.setEnabled(enable);
        newPropertyType.setEnabled(enable);
        newPropertyButton.setEnabled(enable);
    }

    private void resetInput() {
        newPropertyId.setText(null);
        newPropertyLabel.setText(null);
        initTypeList();
        newPropertyType.setSelectedValue(NOT_SELECTED);
    }

    //Event handlers

    @UiHandler("newPropertyButton")
    void newPropertyClick(ClickEvent event) {
        createNewProperty(  dataObject,
                            DataModelerUtils.getInstance().unCapitalize(newPropertyId.getText()),
                            newPropertyLabel.getText(),
                            newPropertyType.getValue() );
    }

    //Event Observers

    private void onDataObjectSelected(@Observes DataObjectSelectedEvent event) {
        if (event.isFrom(getDataModel())) {
            DataObjectTO dataObject = event.getCurrentDataObject();
            resetInput();
            setDataObject(dataObject);
            enableNewPropertyAction(dataObject != null);
        }
    }

    private void onDataObjectCreated(@Observes DataObjectCreatedEvent event) {
        if (event.isFrom(getDataModel())) {
            initTypeList();
            enableNewPropertyAction(true);
        }
    }

    private void onDataObjectDeleted(@Observes DataObjectDeletedEvent event) {
        if (event.isFrom(getDataModel())) {
            initTypeList();
            // When all objects from current model have been deleted clean
            if (getDataModel().getDataObjects().size() == 0) {
                dataObjectPropertiesProvider.getList().clear();
                dataObjectPropertiesProvider.flush();
                dataObjectPropertiesProvider.refresh();
                dataObjectPropertiesTable.redraw();
                objectName.setText(null);
                resetInput();
                enableNewPropertyAction(false);
            }
        }
    }

    private void onDataObjectChange(@Observes DataObjectChangeEvent event) {
        if (event.isFrom(getDataModel())) {
            if ("name".equals(event.getPropertyName()) ||
                "packageName".equals(event.getPropertyName()) ||
                "label".equals(event.getPropertyName())) {

                // For self references: in case name or package changes redraw properties table
                if (dataObject.getClassName().equalsIgnoreCase( event.getCurrentDataObject().getClassName() )) {
                    dataObjectPropertiesProvider.refresh();
                    dataObjectPropertiesTable.redraw();
                }

                objectName.setText(DataModelerUtils.getDataObjectFullLabel(getDataObject()));
                initTypeList();
            }
        }
    }

    private void onDataObjectPropertyChange(@Observes DataObjectFieldChangeEvent event) {
        if (event.isFrom(getDataModel())) {
            if ("name".equals(event.getPropertyName()) ||
                "className".equals(event.getPropertyName()) ||
                "label".equals(event.getPropertyName())) {

                List<ObjectPropertyTO> props = dataObjectPropertiesProvider.getList();
                for (int i = 0; i < props.size(); i++) {
                    if (event.getCurrentField() == props.get(i)) {
                        dataObjectPropertiesTable.redrawRow(i);
                        break;
                    }
                }
            }
        }
    }

    // Event notifications
    private void notifyFieldSelected(ObjectPropertyTO selectedPropertyTO) {
        dataModelerEvent.fire(new DataObjectFieldSelectedEvent(DataModelerEvent.DATA_OBJECT_BROWSER, getDataModel(), getDataObject(), selectedPropertyTO));
    }

    private void notifyFieldDeleted(ObjectPropertyTO deletedPropertyTO) {
        dataModelerEvent.fire(new DataObjectFieldDeletedEvent(DataModelerEvent.DATA_OBJECT_BROWSER, getDataModel(), getDataObject(), deletedPropertyTO));
    }

    private void notifyFieldCreated(ObjectPropertyTO createdPropertyTO) {
        dataModelerEvent.fire(new DataObjectFieldCreatedEvent(DataModelerEvent.DATA_OBJECT_BROWSER, getDataModel(), getDataObject(), createdPropertyTO));
    }

    private void notifyObjectSelected(DataObjectTO dataObject) {
        dataModelerEvent.fire(new DataObjectSelectedEvent(DataModelerEvent.DATA_OBJECT_BROWSER, getDataModel(), dataObject));
    }
}