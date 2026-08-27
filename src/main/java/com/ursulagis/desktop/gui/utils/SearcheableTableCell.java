package com.ursulagis.desktop.gui.utils;

import java.util.function.Predicate;

import org.controlsfx.control.SearchableComboBox;

import impl.org.controlsfx.skin.SearchableComboBoxSkin;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Skin;
import javafx.scene.control.TableCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.beans.binding.Bindings;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Skin;
import javafx.scene.control.SkinBase;
import javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.StringConverter;
import org.controlsfx.control.SearchableComboBox;
import org.controlsfx.control.textfield.CustomTextField;
import org.controlsfx.control.textfield.TextFields;

import java.util.Arrays;
import java.util.function.Predicate;

import static impl.org.controlsfx.i18n.Localization.getString;

import java.util.logging.Logger;
public class SearcheableTableCell <T, C> extends TableCell<T, C> {
	private static final Logger logger = Logger.getLogger(SearcheableTableCell.class.getName());

	//public class BooleanTableCell1<T> extends TableCell<T, C> {
		//Label label = new Label();
		SearchableComboBox<C> searcheableComboBox = null;
		private javafx.beans.value.ChangeListener<C> valueChangeListener;
		/** True while the search filter rebuilds items — avoids committing a spuriously selected value. */
		private boolean suppressCommit;

		//VBox container = null;
		StackPane container = null;
	
		public SearcheableTableCell(ObservableList<C> ops) {
			searcheableComboBox = new SearchableComboBox<>(){
				@Override
				protected Skin<?> createDefaultSkin() {
					return new CustomSearchableComboBoxSkin<>(this);
				}				
			};
		
			searcheableComboBox.setItems(ops);
			VBox.setVgrow(searcheableComboBox, Priority.ALWAYS);
			//container = new VBox(searcheableComboBox);
			container = new StackPane(searcheableComboBox);
			//container.setPadding(new Insets(8));
			setText(null);
			setGraphic(null);
		}

	@Override
	public void startEdit() {
		super.startEdit();
		searcheableComboBox.getSelectionModel().select(getItem());
		setGraphic(container);
		searcheableComboBox.requestFocus();
		setText(null);
		searcheableComboBox.show();
		
		// Remove any existing listener first
		if (valueChangeListener != null) {
			searcheableComboBox.valueProperty().removeListener(valueChangeListener);
		}
		
		// Add new listener
		valueChangeListener = (obs, oldVal, newVal) -> {
			if (newVal != null && !suppressCommit) {
				commitEdit(newVal);
			}
		};
		searcheableComboBox.valueProperty().addListener(valueChangeListener);
	}
	@Override
	public void commitEdit(C value) {
		logger.fine("commitEdit "+value);
		// Remove the listener when committing edit
		if (valueChangeListener != null) {
			searcheableComboBox.valueProperty().removeListener(valueChangeListener);
			valueChangeListener = null;
		}
		super.commitEdit(value);
		//searcheableComboBox.getSelectionModel().select(value);
		setGraphic(null);
		setText(value.toString());		
		//setItem(value);
	}
	@Override
	public void cancelEdit() {
		super.cancelEdit();
		// Remove the listener when canceling edit
		if (valueChangeListener != null) {
			searcheableComboBox.valueProperty().removeListener(valueChangeListener);
			valueChangeListener = null;
		}
		setGraphic(null);
		setText(getItem().toString());
	}
	
	
	
		@Override
		protected void updateItem(C item, boolean empty) {
			super.updateItem(item, empty);
			if (!empty) {
				if (item != null) {
					setText(item.toString());
					searcheableComboBox.getSelectionModel().select(item);//setSelected(item);
				} 				
			} else {
				setText(null);
				setGraphic(null);
			}
	}

    class CustomSearchableComboBoxSkin<C1> extends SkinBase<ComboBox<C1>> {
   
	   private final Image filterIcon = new Image(CustomSearchableComboBoxSkin.class.getResource("/impl/org/controlsfx/table/filter.png").toExternalForm());
   
	   /**
		* A "normal" combobox used internally as a delegate to get the default combo box behavior.
		* This combo box contains the filtered items and handles the popup.
		*/
	   private final ComboBox<C1> filteredComboBox;
   
	   /**
		* The search field shown when the popup is shown.
		*/
	   private final TextField searchField;
   
	   /**
		* Used when pressing ESC
		*/
	   private C1 previousValue;
   
	   public CustomSearchableComboBoxSkin(SearchableComboBox<C1> comboBox) {
		   super(comboBox);
   
		   // first create the filtered combo box
		   filteredComboBox = createFilteredComboBox();
		   getChildren().add(filteredComboBox);
   
		   // and the search field
		   searchField = createSearchField();
		   getChildren().add(searchField);
   
		   bindSearchFieldAndFilteredComboBox();
		   preventDefaultComboBoxKeyListener();
   
		   // open the popup on Cursor Down and up
		   //omboBox.addEventHandler(KeyEvent.KEY_PRESSED, this::checkOpenPopup);
	   }
   
	   @Override
	   protected void layoutChildren(double x, double y, double w, double h) {
		   // allow both components to grow beyond the column width
		   double expandedWidth =w;
		   filteredComboBox.resizeRelocate(x, y, expandedWidth, h);
		   searchField.resizeRelocate(x, y, expandedWidth, h);
	   }
   
	   private TextField createSearchField() {
		   TextField field = new TextField();//(TextField) TextFields.createClearableTextField();
		   field.setPromptText(getString("filterpanel.search.field"));
		   field.setId("search");
		   field.getStyleClass().add("combo-box-search");
		   ImageView imageView = new ImageView(filterIcon);
		   imageView.setFitHeight(15);
		   imageView.setPreserveRatio(true);
		  // field.setLeft(imageView);

		   // ComboBoxListViewSkin treats SPACE as "select and close". Consume KEY_PRESSED so
		   // the space stays in the search text (KEY_TYPED still inserts the character).
		   field.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
			   if (e.getCode() == KeyCode.SPACE) {
				   e.consume();
			   }
		   });
		   
		   return field;
	   }
   
	   private ComboBox<C1> createFilteredComboBox() {
		   ComboBox<C1> box = new ComboBox<>();
		   box.setId("filtered");
		   box.getStyleClass().add("combo-box-filtered");
		   box.setFocusTraversable(false);

		   // unidirectional bindings -- copy values from skinnable
		   Bindings.bindContent(box.getStyleClass(), getSkinnable().getStyleClass());
		   box.buttonCellProperty().bind(getSkinnable().buttonCellProperty());
		   box.cellFactoryProperty().bind(getSkinnable().cellFactoryProperty());
		   box.converterProperty().bind(getSkinnable().converterProperty());
		   box.placeholderProperty().bind(getSkinnable().placeholderProperty());
		   box.disableProperty().bind(getSkinnable().disableProperty());
		   box.visibleRowCountProperty().bind(getSkinnable().visibleRowCountProperty());
		   box.promptTextProperty().bind(getSkinnable().promptTextProperty());
		   getSkinnable().showingProperty().addListener((obs, oldVal, newVal) -> {
			   if (newVal)   box.show();
			   else   box.hide();
		   });
		//    Object cbSkinObj =box.getSkin();
		//    if(cbSkinObj instanceof ComboBoxListViewSkin){
		// 	ComboBoxListViewSkin<?> cbSkin = (ComboBoxListViewSkin) cbSkinObj;
		// 	cbSkin.setHideOnClick(false);
		//    }
		   

		   // bidirectional bindings
		   box.valueProperty().bindBidirectional(getSkinnable().valueProperty());

		   // Allow the combobox to grow beyond the column width
		   box.setMinWidth(200); // Set a reasonable minimum width
		   box.setMaxWidth(Double.MAX_VALUE);
		   box.setPrefWidth(300); // Set a preferred width that's larger than most columns

		   return box;
	   }
   
	   private void bindSearchFieldAndFilteredComboBox() {
		   // set the items of the filtered combo box
		   filteredComboBox.setItems(createFilteredList());
		   // and keep it up to date, even if the original list changes
		   getSkinnable().itemsProperty()
				   .addListener((obs, oldVal, newVal) -> filteredComboBox.setItems(createFilteredList()));
		   // and update the filter, when the text in the search field changes
	
		   searchField.textProperty().addListener(o -> {			
			updateFilter();
		});
   
		   // the search field must only be visible, when the popup is showing
		   searchField.visibleProperty().bind(filteredComboBox.showingProperty());
   
		   filteredComboBox.showingProperty().addListener((obs, oldVal, newVal) ->
		   {
			   if (newVal) {
				   // When the filtered combo box popup is showing, we must also set the showing property
				   // of the original combo box. And here we must remember the previous value for the
				   // ESCAPE behavior. And we must transfer the focus to the search field, because
				   // otherwise the search field would not allow typing in the search text.
				   getSkinnable().show();
				   previousValue = getSkinnable().getValue();
				   searchField.requestFocus();
			   } else {
				   // When the filtered combo box popup is hidden, we must also set the showing property
				   // of the original combo box to false, clear the search field.
				   //getSkinnable().hide();
				  // searchField.setText("");
			   }
		   });
   
		   // but when the search field is focussed, the popup must still be shown
		   searchField.focusedProperty().addListener((obs, oldVal, newVal) ->
		   {
			   if (newVal) {
				   filteredComboBox.show();
			   } else {
				   // Only hide if the popup is actually supposed to be hidden
				   // Don't hide just because focus is lost temporarily
				   // The popup will be hidden by other mechanisms (Enter, Escape, etc.)
			   }
		   });
	   }
   
	   private FilteredList<C1> createFilteredList() {

		   return new FilteredList<C1>(getSkinnable().getItems(), predicate());
	   }
   
	   /**
		* Called every time the filter text changes.
		*/
	   private void updateFilter() {
		   // does not work, because of Bug https://bugs.openjdk.java.net/browse/JDK-8174176
		   // ((FilteredList<C1>)filteredComboBox.getItems()).setPredicate(predicate());
		   suppressCommit = true;
		   try {
			   C1 retained = filteredComboBox.getValue();
			   FilteredList<C1> filteredList = createFilteredList();
			   filteredComboBox.setItems(filteredList);
			   // Keep the current value so setItems does not auto-select another row and commit.
			   filteredComboBox.setValue(retained);
		   } finally {
			   suppressCommit = false;
		   }
	   }
   
	   /**
		* Return the Predicate to filter the popup items based on the search field.
		*/
	   private Predicate<C1> predicate() {
		   String searchText = searchField.getText() == null ? "" : searchField.getText().trim();
		   if (searchText.isEmpty()) {
			   return null;
		   }
		   return predicate(searchText);
	   }
   
	   /**
		* Return the Predicate to filter the popup items based on the given search text.
		* Matches when the display text contains every word (spaces allowed in the query).
		*/
	   private Predicate<C1> predicate(String searchText) {
		   String[] lowerCaseSearchWords = searchText.toLowerCase().split("\\s+");
		   return value -> {
			   String lowerCaseDisplayText = getDisplayText(value);
			   if (lowerCaseDisplayText == null) {
				   return false;
			   }
			   String haystack = lowerCaseDisplayText.toLowerCase();
			   return Arrays.stream(lowerCaseSearchWords)
					   .filter(word -> !word.isEmpty())
					   .allMatch(haystack::contains);
		   };
	   }
   
	   /**
		* Create a text for the given item, that can be used to compare with the filter text.
		*/
	   private String getDisplayText(C1 value) {
		   StringConverter<C1> converter = filteredComboBox.getConverter();
		   return value == null ? "" : (converter != null ? converter.toString(value) : value.toString());
	   }
   
	   /**
		* The default behavior of the ComboBoxListViewSkin is to close the popup on
		* ENTER and SPACE, but we need to override this behavior.
		*/
	   private void preventDefaultComboBoxKeyListener() {
		   filteredComboBox.skinProperty().addListener((obs, oldVal, newVal) -> installListViewKeyHandler());
		   // Popup content is often created lazily; re-install when the popup opens.
		   filteredComboBox.showingProperty().addListener((obs, oldVal, newVal) -> {
			   if (Boolean.TRUE.equals(newVal)) {
				   installListViewKeyHandler();
			   }
		   });
	   }

	   private void installListViewKeyHandler() {
		   Skin<?> skin = filteredComboBox.getSkin();
		   if (!(skin instanceof ComboBoxListViewSkin)) {
			   return;
		   }
		   ComboBoxListViewSkin<?> cblwSkin = (ComboBoxListViewSkin<?>) skin;
		   if (cblwSkin.getPopupContent() instanceof ListView) {
			   @SuppressWarnings("unchecked")
			   final ListView<C1> listView = (ListView<C1>) cblwSkin.getPopupContent();
			   listView.setOnKeyPressed(this::checkApplyAndCancel);
		   }
	   }
   
	   /**
		* Used to alter the behaviour. React on Enter, Tab and ESC.
		* SPACE must not close the popup — it belongs in the search field.
		*/
	   private void checkApplyAndCancel(KeyEvent e) {
		   KeyCode code = e.getCode();
		   if (code == KeyCode.ENTER || code == KeyCode.TAB) {
			   // select the first item if no selection
			   if (filteredComboBox.getSelectionModel().isEmpty()){
					filteredComboBox.getSelectionModel().selectFirst();
			   }
			   getSkinnable().hide();
			   if (code == KeyCode.ENTER) {
				   // otherwise the focus would be somewhere else
				   getSkinnable().requestFocus();
			   }
		   } else if (code == KeyCode.ESCAPE) {
			   getSkinnable().setValue(previousValue);
			   getSkinnable().hide();
			   // otherwise the focus would be somewhere else
			   getSkinnable().requestFocus();
		   } else if (code == KeyCode.SPACE) {
			   // Do not close the combo; type a space into the search field.
			   searchField.requestFocus();
			   searchField.replaceSelection(" ");
			   e.consume();
		   }
		   // Arrow / other keys: leave to ListView default navigation
	   }
   
	   /**
		* Show the popup on UP, DOWN, and on beginning typing a word.
		*/
	   private void checkOpenPopup(KeyEvent e) {
		   KeyCode code = e.getCode();
		   if (code == KeyCode.UP || code == KeyCode.DOWN) {
			   filteredComboBox.show();
			   // only open the box navigation
			   e.consume();
		   } else if (code.isLetterKey() || code.isDigitKey() || code == KeyCode.SPACE) {
			   // show the box, let the box handle the KeyEvent
			   filteredComboBox.show();
		   }
	   }
   
   }
   
}

