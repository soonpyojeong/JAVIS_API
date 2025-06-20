import { TreeState, TreeViewItem } from "../types";
import { Ref } from "vue";
export declare function uuidv4(): string;
/**
 * Initialises the root state of a tree.
 * @param itemSelectedEventHandler A callback when an item is selected.
 * @param itemSelectedEventHandler A callback when an item is checked.
 * @returns
 */
export declare function useGraph(items: Ref<TreeViewItem[]>, itemSelectedEventHandler: (selectedItem: TreeViewItem) => void, itemCheckedEventHandler: (checkedItems: TreeViewItem[]) => void, itemExpandedEventHandler: (expandedItem: TreeViewItem) => void, itemCollapsedEventHandler: (collapsedItem: TreeViewItem) => void): TreeState;
