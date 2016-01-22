/**
 * Licensed to JumpMind Inc under one or more contributor
 * license agreements.  See the NOTICE file distributed
 * with this work for additional information regarding
 * copyright ownership.  JumpMind Inc licenses this file
 * to you under the GNU General Public License, version 3.0 (GPLv3)
 * (the "License"); you may not use this file except in compliance
 * with the License.
 *
 * You should have received a copy of the GNU General Public License,
 * version 3.0 (GPLv3) along with this library; if not, see
 * <http://www.gnu.org/licenses/>.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jumpmind.metl.ui.views.design;

import static org.apache.commons.lang.StringUtils.isBlank;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.jumpmind.metl.core.model.ComponentAttributeSetting;
import org.jumpmind.metl.core.model.ComponentEntitySetting;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelAttribute;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.runtime.component.DataDiff;
import org.jumpmind.metl.ui.common.ButtonBar;
import org.jumpmind.metl.ui.common.UiUtils;
import org.jumpmind.vaadin.ui.common.ResizableWindow;

import com.vaadin.data.Container;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.event.Transferable;
import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.DropHandler;
import com.vaadin.event.dd.acceptcriteria.AcceptAll;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.AbstractSelect.AbstractSelectTargetDetails;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Field;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.CellStyleGenerator;
import com.vaadin.ui.Table.ColumnGenerator;
import com.vaadin.ui.Table.TableDragMode;
import com.vaadin.ui.TableFieldFactory;
import com.vaadin.ui.TextField;

@SuppressWarnings("serial")
public class EditDataDiffPanel extends AbstractComponentEditPanel {

    private static final long serialVersionUID = 1L;
    
    Table entityTable = new Table();
    BeanItemContainer<EntitySettings> entitySettingsContainer = new BeanItemContainer<EntitySettings>(EntitySettings.class);
    List<EntitySettings> entitySettings = new ArrayList<EntitySettings>();
    
    Table attributeTable = new Table();
    BeanItemContainer<AttributeSettings> attributeSettingsContainer = new BeanItemContainer<AttributeSettings>(AttributeSettings.class);
    List<AttributeSettings> attributeSettings = new ArrayList<AttributeSettings>();

    TextField entityFilterField;

    Button editButton;
    
    EditAttributesWindow attributeWindow;
    
    Set<EntitySettings> selectedItemIds;
    
    protected void buildUI() {

    	buildButtonBar();
    	buildEntityTable();
    	fillEntityContainer();
        updateEntityTable(null);
        buildAttributeWindow();
    }

    protected void buildButtonBar() {
        ButtonBar buttonBar = new ButtonBar();
        addComponent(buttonBar);
        editButton = buttonBar.addButton("Edit Columns", FontAwesome.EDIT);
        editButton.addClickListener(new EditButtonClickListener());
        entityFilterField = buttonBar.addFilter();
        entityFilterField.addTextChangeListener(event -> updateEntityTable(event.getText()));
        
        Button moveUpButton = buttonBar.addButton("Move Up", FontAwesome.ARROW_UP);
        moveUpButton.addClickListener(new MoveUpClickListener());

        Button moveDownButton = buttonBar.addButton("Move Down", FontAwesome.ARROW_DOWN);
        moveDownButton.addClickListener(new MoveDownClickListener());

        Button moveTopButton = buttonBar.addButton("Move Top", FontAwesome.ANGLE_DOUBLE_UP);
        moveTopButton.addClickListener(new MoveTopClickListener());

        Button moveBottomButton = buttonBar.addButton("Move Bottom", FontAwesome.ANGLE_DOUBLE_DOWN);
        moveBottomButton.addClickListener(new MoveBottomClickListener());

        Button cutButton = buttonBar.addButton("Cut", FontAwesome.CUT);
        cutButton.addClickListener(new CutClickListener());

        Button pasteButton = buttonBar.addButton("Paste", FontAwesome.PASTE);
        pasteButton.addClickListener(new PasteClickListener());


        addComponent(buttonBar);    	
    }

    @SuppressWarnings("unchecked")
    protected Set<EntitySettings> getSelectedItems() {
        return (Set<EntitySettings>) entityTable.getValue();
    }
    
    protected EntitySettings getSelectedItem() {
        Set<EntitySettings> selectedItems = getSelectedItems();
        if (selectedItems != null && selectedItems.size() == 1) {
            return selectedItems.iterator().next();
        }
        return null;
    }
    
    class EditButtonClickListener implements ClickListener {
        private static final long serialVersionUID = 1L;
        public void buttonClick(ClickEvent event) {
            if (getSelectedItem() != null) {
            	refreshAttributeContainer((EntitySettings) getSelectedItem());
            	updateAttributeTable();
                attributeWindow.show();
            }
        }   
    }    
    
    protected void buildEntityTable() {
        entityTable.setContainerDataSource(entitySettingsContainer);
        entityTable.setSelectable(true);
        entityTable.setSortEnabled(false);
        entityTable.setImmediate(true);
        entityTable.setSizeFull();
        entityTable.addGeneratedColumn("entityName", new ColumnGenerator() {
            private static final long serialVersionUID = 1L;
            @Override
            public Object generateCell(Table source, Object itemId, Object columnId) {
                EntitySettings setting = (EntitySettings) itemId;
                Model model = component.getInputModel();
                ModelEntity entity = model.getEntityById(setting.getEntityId());
                return UiUtils.getName(entityFilterField.getValue(), entity.getName());
            }
        });
        entityTable.setVisibleColumns(new Object[] { "entityName", "addEnabled", "updateEnabled", "deleteEnabled" });
        entityTable.setColumnWidth("entityName", 250);
        entityTable.setColumnHeaders(new String[] { "Entity Name", "Add Enabled", "Chg Enabled", "Del Enabled" });
        entityTable.setColumnExpandRatio("entityName", 1);
        entityTable.setTableFieldFactory(new EditEntityFieldFactory());
        entityTable.setEditable(true);
        entityTable.setMultiSelect(true);
        entityTable.setDragMode(TableDragMode.MULTIROW);
        entityTable.setDropHandler(new TableDropHandler());
        entityTable.setCellStyleGenerator(new TableCellStyleGenerator());
        addComponent(entityTable);        
        setExpandRatio(entityTable, 1.0f);
    }    
    
    class TableCellStyleGenerator implements CellStyleGenerator {
        public String getStyle(Table source, Object itemId, Object propertyId) {
            if (propertyId != null && selectedItemIds != null && selectedItemIds.contains(itemId)) {
                return "highlight";
            }
            return null;
        }
    }

    protected void fillEntityContainer() {  	
        if (component.getInputModel() != null) {

            for (ModelEntity entity : component.getInputModel().getModelEntities()) {
                ComponentEntitySetting insert = component.getSingleEntitySetting(entity.getId(), DataDiff.ENTITY_ADD_ENABLED);
                ComponentEntitySetting update = component.getSingleEntitySetting(entity.getId(), DataDiff.ENTITY_CHG_ENABLED);
                ComponentEntitySetting delete = component.getSingleEntitySetting(entity.getId(), DataDiff.ENTITY_DEL_ENABLED);
                ComponentEntitySetting ordinal = component.getSingleEntitySetting(entity.getId(), DataDiff.ENTITY_ORDER);
                boolean insertEnabled = insert != null ? Boolean.parseBoolean(insert.getValue()) : true;
                boolean updateEnabled = update != null ? Boolean.parseBoolean(update.getValue()) : true;
                boolean deleteEnabled = delete != null ? Boolean.parseBoolean(delete.getValue()) : true;
                Integer order = ordinal != null ? Integer.parseInt(ordinal.getValue()) : 0;
                entitySettings.add(new EntitySettings(entity.getId(), insertEnabled, updateEnabled, deleteEnabled, 
                        order));
            }
            
            Collections.sort(entitySettings, new Comparator<EntitySettings>() {
                public int compare(EntitySettings o1, EntitySettings o2) {
                    return new Integer(o1.getOrdinalSetting()).compareTo(new Integer(o2.getOrdinalSetting()));
                }
            });
        }    	
    }
    
    protected void updateEntityTable(String filter) {
        filter = filter != null ? filter.toLowerCase() : null;
        entityFilterField.setValue(filter);
        entityTable.removeAllItems();
        for (EntitySettings entitySetting : entitySettings) {
            Model model = component.getInputModel();
            ModelEntity entity = model.getEntityById(entitySetting.getEntityId());
            if (isBlank(filter) || entity.getName().toLowerCase().contains(filter)) {
            	entityTable.addItem(entitySetting);
            }
        }
    }
    
    protected void moveItemsTo(Set<EntitySettings> itemIds, int index) {
        if (index >= 0 && index < entitySettingsContainer.getItemIds().size() && itemIds.size() > 0) {
            int firstItemIndex = entitySettingsContainer.indexOfId(itemIds.iterator().next());
            if (index != firstItemIndex) {
                for (EntitySettings itemId : itemIds) {
                    boolean movingUp = index < entitySettingsContainer.indexOfId(itemId);
                    entitySettingsContainer.removeItem(itemId);
                    entitySettingsContainer.addItemAt(index, itemId);
                    if (movingUp) {
                        index++;
                    }
                }
                calculatePositions();
                saveOrdinalSettings();
            }
        }
    }
    
    protected void saveOrdinalSettings() {
        String attrName = DataDiff.ENTITY_ORDER;
        int ordinal = 1;
        for (EntitySettings record : entitySettingsContainer.getItemIds()) {
            saveSetting(record.getEntityId(), attrName, String.valueOf(ordinal));
            ordinal++;
        }
    }
         
    protected void saveSetting(String entityId, String name, String value) {
        ComponentEntitySetting setting = component.getSingleEntitySetting(entityId, name);
        if (setting == null) {
            setting = new ComponentEntitySetting(entityId, name, value);
            setting.setComponentId(component.getId());
            component.addEntitySetting(setting);
            context.getConfigurationService().save(setting);
        } else if (!StringUtils.equals(setting.getValue(), value)) {
            setting.setValue(value);
            context.getConfigurationService().save(setting);
        }
    }

    protected void calculatePositions() {
        boolean needsRefreshed = false;

        int ordinal = 1;
        for (EntitySettings record : entitySettingsContainer.getItemIds()) {
            if (record.getOrdinalSetting() != ordinal) {
                record.setOrdinalSetting(ordinal);
                needsRefreshed = true;
            }
            ordinal++;
        }
        if (needsRefreshed) {
            entityTable.refreshRowCache();
        }
    }
    
    class EditEntityFieldFactory implements TableFieldFactory {
        private static final long serialVersionUID = 1L;
        public Field<?> createField(final Container dataContainer, final Object itemId, final Object propertyId,
                com.vaadin.ui.Component uiContext) {
            final EntitySettings settings = (EntitySettings) itemId;

            if (propertyId.equals("addEnabled")) {
                return createEntityCheckBox(settings, DataDiff.ENTITY_ADD_ENABLED);
            } else if (propertyId.equals("updateEnabled")) {
                return createEntityCheckBox(settings, DataDiff.ENTITY_CHG_ENABLED);
            } else if (propertyId.equals("deleteEnabled")) {
                return createEntityCheckBox(settings, DataDiff.ENTITY_DEL_ENABLED);
            } else {
                return null;
            }
        }
    }

    protected CheckBox createEntityCheckBox(final EntitySettings settings, final String key) {
        final CheckBox checkBox = new CheckBox();
        checkBox.setImmediate(true);
        checkBox.addValueChangeListener(new ValueChangeListener() {
            private static final long serialVersionUID = 1L;
            @Override
            public void valueChange(ValueChangeEvent event) {
                ComponentEntitySetting setting = component.getSingleEntitySetting(settings.getEntityId(), key);

                String oldValue = setting == null ? Boolean.TRUE.toString() : setting.getValue();
                if (setting == null) {
                    setting = new ComponentEntitySetting(settings.getEntityId(), component.getId(), key, Boolean.TRUE.toString());
                    component.addEntitySetting(setting);
                }
                setting.setValue(checkBox.getValue().toString());
                if (!oldValue.equals(setting.getValue())) {
                    context.getConfigurationService().save(setting);
                }
            }
        });
        return checkBox;
    }

    public static class EntitySettings implements Serializable {
        private static final long serialVersionUID = 1L;
        String entityId;
        boolean addEnabled;
        boolean updateEnabled;
        boolean deleteEnabled;
        int ordinalSetting;

        public EntitySettings(String entityId, boolean insertEnabled, boolean updateEnabled, boolean deleteEnabled, int ordinal) {
            this.entityId = entityId;
            this.addEnabled = insertEnabled;
            this.updateEnabled = updateEnabled;
            this.deleteEnabled = deleteEnabled;
            this.ordinalSetting = ordinal;
        }

        public void setAddEnabled(boolean insertEnabled) {
            this.addEnabled = insertEnabled;
        }

        public boolean isAddEnabled() {
            return addEnabled;
        }

        public void setUpdateEnabled(boolean updateEnabled) {
            this.updateEnabled = updateEnabled;
        }

        public boolean isUpdateEnabled() {
            return updateEnabled;
        }

        public void setDeleteEnabled(boolean deleteEnabled) {
            this.deleteEnabled = deleteEnabled;
        }

        public boolean isDeleteEnabled() {
            return deleteEnabled;
        }

        public String getEntityId() {
            return entityId;
        }

        public void setEntityId(String entityId) {
            this.entityId = entityId;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof EntitySettings) {
                return entityId.equals(((EntitySettings) obj).getEntityId());
            } else {
                return super.equals(obj);
            }
        }

        @Override
        public int hashCode() {
            return entityId.hashCode();
        }

        public int getOrdinalSetting() {
            return ordinalSetting;
        }

        public void setOrdinalSetting(int ordinalSetting) {
            this.ordinalSetting = ordinalSetting;
        }
    }

    //attribute window and support
    
    protected void buildAttributeWindow() {
        attributeWindow = new EditAttributesWindow();
    }
    
    class EditAttributesWindow extends ResizableWindow {
        private static final long serialVersionUID = 1L;
		public EditAttributesWindow() {
			super("Edit Columns to Compare");
			setWidth(800f, Unit.PIXELS);
			setHeight(600f, Unit.PIXELS);
			content.setMargin(true);
			buildAttributeTable();
			addComponent(buildButtonFooter(buildCloseButton()));
		}
    	
        private void buildAttributeTable() {
     	   
     	   attributeTable.setContainerDataSource(attributeSettingsContainer);
     	   attributeTable.setSelectable(true);
     	   attributeTable.setSortEnabled(false);
     	   attributeTable.setImmediate(true);
     	   attributeTable.setSortEnabled(false);
     	   attributeTable.setSizeFull();
     	   attributeTable.addGeneratedColumn("attributeName", new ColumnGenerator() {
     	      private static final long serialVersionUID = 1L;
                @Override
                public Object generateCell(Table source, Object itemId, Object columnId) {
             	   AttributeSettings setting = (AttributeSettings) itemId;
                    Model model = component.getInputModel();
                    ModelAttribute attribute = model.getAttributeById(setting.getAttributeId());
                    return UiUtils.getName(entityFilterField.getValue(), attribute.getName());
                }
            });
     	   attributeTable.setVisibleColumns(new Object[] { "attributeName", "compareEnabled" });
     	   attributeTable.setColumnWidth("attributeName", 250);
     	   attributeTable.setColumnHeaders(new String[] { "Attribute Name", "Compare Enabled" });
     	   attributeTable.setColumnExpandRatio("attributeName", 1);
     	   attributeTable.setTableFieldFactory(new EditAttributeFieldFactory());
     	   attributeTable.setEditable(true);
            addComponent(attributeTable, 1);
            
        }
    }
        
    private void refreshAttributeContainer(EntitySettings selectedRow) {
  	   attributeSettings.clear();
  	   ModelEntity entity = component.getInputModel().getEntityById(selectedRow.getEntityId());
  	   for (ModelAttribute attribute : entity.getModelAttributes()) {
  		   
            ComponentAttributeSetting compare = component.getSingleAttributeSetting(attribute.getId(), DataDiff.ATTRIBUTE_COMPARE_ENABLED);
            boolean compareEnabled = compare != null ? Boolean.parseBoolean(compare.getValue()) : true;
            attributeSettings.add(new AttributeSettings(attribute.getId(), compareEnabled, attribute.isPk() == true?true:false));     		        		   
  	   }
     }

    protected void updateAttributeTable() {
        attributeTable.removeAllItems();
        for (AttributeSettings attributeSetting : attributeSettings) {
            attributeTable.addItem(attributeSetting);
        }
    }    
    
    class EditAttributeFieldFactory implements TableFieldFactory {
        private static final long serialVersionUID = 1L;
        public Field<?> createField(final Container dataContainer, final Object itemId, final Object propertyId,
                com.vaadin.ui.Component uiContext) {
            final AttributeSettings settings = (AttributeSettings) itemId;
            if (propertyId.equals("compareEnabled")) {
                return createAttributeCheckBox(settings, DataDiff.ATTRIBUTE_COMPARE_ENABLED);
            } else {
                return null;
            }
        }
    }
    
    protected CheckBox createAttributeCheckBox(final AttributeSettings settings, final String key) {
        final CheckBox checkBox = new CheckBox();
        checkBox.setImmediate(true);
        if (settings.isPrimaryKey()) {
        	checkBox.setEnabled(false);
        }
        checkBox.addValueChangeListener(new ValueChangeListener() {
            private static final long serialVersionUID = 1L;
            @Override
            public void valueChange(ValueChangeEvent event) {
                ComponentAttributeSetting setting = component.getSingleAttributeSetting(settings.getAttributeId(), key);

                String oldValue = setting == null ? Boolean.TRUE.toString() : setting.getValue();
                if (setting == null) {
                    setting = new ComponentAttributeSetting(settings.getAttributeId(), component.getId(), key, Boolean.TRUE.toString());
                    component.addAttributeSetting(setting);
                }
                setting.setValue(checkBox.getValue().toString());
                if (!oldValue.equals(setting.getValue())) {
                    context.getConfigurationService().save(setting);
                }
            }
        });
        return checkBox;
    }
    
    public static class AttributeSettings implements Serializable {
        private static final long serialVersionUID = 1L;
        boolean primaryKey;
        String attributeId;
        boolean compareEnabled;

        public AttributeSettings(String attributeId, boolean compareEnabled, boolean primaryKey) {
            this.attributeId = attributeId;
            this.compareEnabled = compareEnabled;
            this.primaryKey = primaryKey;
        }

        public void setCompareEnabled(boolean compareEnabled) {
            this.compareEnabled = compareEnabled;
        }

        public boolean isCompareEnabled() {
            return compareEnabled;
        }

        public String getAttributeId() {
            return attributeId;
        }

        public void setAttributeId(String attributeId) {
            this.attributeId = attributeId;
        }
        
        public void setPrimaryKey(boolean primaryKey) {
        	this.primaryKey = primaryKey;
        }
        
        public boolean isPrimaryKey() {
        	return primaryKey;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof AttributeSettings) {
                return attributeId.equals(((AttributeSettings) obj).getAttributeId());
            } else {
                return super.equals(obj);
            }
        }   
    }

    class MoveUpClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            Set<EntitySettings> itemIds = getSelectedItems();
            if (itemIds.size() > 0 && itemIds != null) {
                EntitySettings firstItem = itemIds.iterator().next();
                int index = entitySettingsContainer.indexOfId(firstItem) - 1;
                moveItemsTo(getSelectedItems(), index);
            }
        }
    }

    class MoveDownClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            Set<EntitySettings> itemIds = getSelectedItems();
            if (itemIds.size() > 0 && itemIds != null) {
                EntitySettings lastItem = null;
                Iterator<EntitySettings> iter = itemIds.iterator();
                while (iter.hasNext()) {
                    lastItem = iter.next();
                }
                int index = entitySettingsContainer.indexOfId(lastItem) + 1;
                moveItemsTo(getSelectedItems(), index);
            }
        }
    }

    class MoveTopClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            moveItemsTo(getSelectedItems(), 0);
        }
    }

    class MoveBottomClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            moveItemsTo(getSelectedItems(), entitySettingsContainer.size() - 1);
        }
    }

    class CutClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            Set<EntitySettings> itemIds = getSelectedItems();
            selectedItemIds = new LinkedHashSet<EntitySettings>(itemIds);
            for (EntitySettings itemId : itemIds) {
                entityTable.unselect(itemId);
            }
            entityTable.refreshRowCache();
        }
    }

    class PasteClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            Set<EntitySettings> itemIds = getSelectedItems();
            if (itemIds.size() > 0 && selectedItemIds != null) {
                int index = entitySettingsContainer.indexOfId(itemIds.iterator().next());
                moveItemsTo(selectedItemIds, index);
                selectedItemIds = null;
            }
        }
    }
    
    class TableDropHandler implements DropHandler {
        public void drop(DragAndDropEvent event) {
            AbstractSelectTargetDetails targetDetails = (AbstractSelectTargetDetails) event.getTargetDetails();
            Transferable transferable = event.getTransferable();
            if (transferable.getSourceComponent() == entityTable) {
                EntitySettings target = (EntitySettings) targetDetails.getItemIdOver();
                moveItemsTo(getSelectedItems(), entitySettingsContainer.indexOfId(target));
            }
        }

        public AcceptCriterion getAcceptCriterion() {
            return AcceptAll.get();
        }
    }
}
