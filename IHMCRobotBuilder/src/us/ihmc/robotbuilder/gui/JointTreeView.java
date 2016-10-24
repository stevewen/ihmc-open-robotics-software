package us.ihmc.robotbuilder.gui;

import javafx.collections.ObservableList;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javaslang.collection.Map;
import org.jetbrains.annotations.Nullable;
import us.ihmc.robotbuilder.util.*;
import us.ihmc.robotbuilder.util.TreeDifference.DifferencesByNode;
import us.ihmc.robotbuilder.util.TreeFocus.ChildIndexOf;
import us.ihmc.robotics.immutableRobotDescription.JointDescription;

import java.util.List;
import java.util.Optional;

import static java.lang.Integer.compare;
import static us.ihmc.robotbuilder.util.TreeFocus.pathTo;

/**
 * Represents a tree view of a robot tree (see {@link Tree<JointDescription>}).
 */
public class JointTreeView extends TreeView<JointDescription>
{
   private TreeMapping<Tree<JointDescription>, JointTreeItem> treeToItemMapping = new TreeMapping<>(JointTreeView::mapTree);

   public JointTreeView()
   {
      setCellFactory(JointTreeCell::new);
   }

   private static JointTreeItem mapTree(Tree<JointDescription> node, List<JointTreeItem> children)
   {
      JointTreeItem item = new JointTreeItem(node);
      item.getChildren().addAll(children);
      item.setExpanded(true);
      return item;
   }

   private void applyDifferencesByNode(JointTreeItem parentItem, DifferencesByNode<JointDescription> differencesByNode)
   {
      differencesByNode.get(parentItem.getOriginalTree()).ifPresent(diff -> {
         parentItem.getChildrenSpecific().forEach(child -> applyDifferencesByNode(child, differencesByNode));

         treeToItemMapping.replaceSingleNode(parentItem.getOriginalTree(), diff.getNewNode());
         parentItem.setOriginalTree(diff.getNewNode());
         parentItem.getChildren().addAll(diff.getAddedChildren().map(treeToItemMapping::mapNewNode).toJavaList());
         parentItem.getChildrenSpecific().removeIf(child -> diff.getRemovedChildren().contains(child.getOriginalTree()));
         diff.getRemovedChildren().forEach(treeToItemMapping::removeMapping);

         // Reorder
         Map<Tree<JointDescription>, Integer> newOrder = diff.getFinalChildOrder();
         parentItem.getChildrenSpecific().sort((item1, item2) -> compare(newOrder.apply(item1.getOriginalTree()), newOrder.apply(item2.getOriginalTree())));
      });
   }

   public FunctionalObservableValue<TreeFocus<Tree<JointDescription>>> selectedNodeObservable()
   {
      ChildIndexOf<TreeItem<JointDescription>> childIndexOf = (tree, child) -> tree.getChildren().indexOf(child);

      return FunctionalObservableValue.of(getSelectionModel().selectedItemProperty())
                                      .avoidCycles()
                                      .narrow(JointTreeItem.class)
                                      .flatMapOptional(selectedItem -> {
                                         JointTreeItem root = rootOf(selectedItem);
                                         TreeFocus<Tree<JointDescription>> rootFocus = root.getOriginalTree().getFocus();
                                         Optional<TreeFocus<Tree<JointDescription>>> result = rootFocus
                                               .focusOnPath(pathTo(selectedItem, childIndexOf, TreeItem::getParent));

                                         assert result.isPresent();
                                         return result;
                                      });
   }

   private static JointTreeItem rootOf(JointTreeItem item)
   {
      JointTreeItem result = item;
      while (result.getParent() instanceof JointTreeItem)
      {
         result = (JointTreeItem)result.getParent();
      }
      return result;
   }

   /**
    * Change the tree displayed in the TreeView to the given new focus.
    * The selected item will be set to whatever node the focus is pointing at.
    * @param newFocus new focused tree
    */
   @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
   public void setFocus(Optional<TreeFocus<Tree<JointDescription>>> newFocus)
   {
      newFocus.flatMap(focus ->
                       {
                          setRootJoint(focus.root().getFocusedNode());
                          return treeToItemMapping.get(focus.getFocusedNode());
                       })
              .ifPresent(getSelectionModel()::select);

      if (!newFocus.isPresent())
         setRootJoint(null);
   }

   /**
    * Change the root joint of this tree view.
    * The new root will be diffed against the existing tree and
    * only necessary changes will be applied to the tree.
    * @param newRoot new root
    */
   private void setRootJoint(@Nullable Tree<JointDescription> newRoot)
   {
      if (newRoot == null)
      {
         setRoot(null);
         return;
      }
      if (!(getRoot() instanceof JointTreeItem))
      {
         setRoot(treeToItemMapping.mapNewNode(newRoot));
         return;
      }
      JointTreeItem itemRoot = (JointTreeItem) getRoot();
      applyDifferencesByNode(itemRoot, TreeDifference.difference(itemRoot.getOriginalTree(), newRoot));
   }

   private static class JointTreeCell extends TreeCell<JointDescription>
   {
      JointTreeCell(TreeView<JointDescription> tree) {
      }

      @Override protected void updateItem(JointDescription item, boolean empty)
      {
         super.updateItem(item, empty);

         if (empty)
         {
            setText(null);
         }
         else
         {
            setText(getItem() == null ? "" : getItem().getName());
         }
         setGraphic(null);
      }
   }

   private static class JointTreeItem extends TreeItem<JointDescription>
   {
      private Tree<JointDescription> originalTree;

      JointTreeItem(Tree<JointDescription> originalTree)
      {
         super(originalTree.getValue());
         this.originalTree = originalTree;
      }

      Tree<JointDescription> getOriginalTree()
      {
         return originalTree;
      }

      void setOriginalTree(Tree<JointDescription> originalTree)
      {
         this.originalTree = originalTree;
         this.setValue(originalTree.getValue());
      }

      ObservableList<JointTreeItem> getChildrenSpecific()
      {
         //noinspection unchecked
         return (ObservableList)getChildren();
      }
   }
}
