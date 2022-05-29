
package cl.netgamer.clicksortlite;

import java.util.Arrays;
import java.util.Comparator;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.inventory.AbstractHorseInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;


public final class Main extends JavaPlugin implements Listener
{
	
	// new labels since v2: fixes, enhances, adds
	
	
	@Override
	public void onEnable()
	{
		getServer().getPluginManager().registerEvents(this, this);
	}
	
	
	@EventHandler(priority = EventPriority.MONITOR)
	void onInventoryClick(InventoryClickEvent e)
	{
		if ( e.isCancelled() )
			return;
		
		// skip any other event than player's dummy middle click in a container
		if ( e.getClick() != ClickType.MIDDLE || e.getSlotType() != SlotType.CONTAINER || !(e.getWhoClicked() instanceof Player) || e.getAction() != InventoryAction.NOTHING )
			return;
		
		// permission added from v3, suggested by SkitAsul@github.com
		Player player = (Player) e.getWhoClicked();
		if ( player.isPermissionSet("clicksortlite") && !player.hasPermission("clicksortlite") )
			return;
		
		// sorry, no creative inventory support because it is client managed and poorly informed
		// pointless anyway because is already arranged
		// however you can sort player inventory in creative mode by sorting bottom inventory from chest views
		
		// get upper inventory, or lower if player clicked below
		Inventory chest = e.getInventory();
		if ( e.getRawSlot() >= chest.getSize() )
			chest = e.getView().getBottomInventory();
		
		// the code below is because Inventory.getStorageContents() don't always works as described
		
		// get storage section according inventory type
		int from = 0, size = chest.getSize();
		if ( chest.getType() == InventoryType.PLAYER )
		{
			from = 9;
			size = 27;
		}
		else if ( chest instanceof AbstractHorseInventory )
		{
			from = 2;
			// enhances: v1 some performance improvements (break loops timely)
			if ( (size -= 2) < 1 )
				return;
		}
		
		// get storage contents
		ItemStack[] contents = chest.getContents();
		ItemStack[] storage = new ItemStack[size];
		System.arraycopy(contents, from, storage, 0, size);
		
		// sort items
		Arrays.sort(storage, new Comparator<ItemStack>()
		{
			@Override
			public int compare(ItemStack item1, ItemStack item2)
			{
				// prevent contract violation, read java8 Comparator javadocs
				// fixes: v1 exception with items in 2nd half large chests
				if ( item1 == null ? item1 == item2 : item1.equals(item2) )
					return 0;
				
				// consider air as the greatest item, pushing it to the last
				if ( isEmptyItem(item1) )
					return 1;
				if ( isEmptyItem(item2) )
					return -1;
				
				// sort criterias from general to particular
				
				//// sort by material numeric id
				//int diff = item1.getType().getId() - item2.getType().getId();
				
				// sort by material name
				int diff = item1.getType().compareTo(item2.getType());
				if ( diff != 0)
					return diff;
				
				// then sort by material data
				diff = item1.getData().getData() - item2.getData().getData();
				if ( diff != 0 )
					return diff;
				
				// then sort by item meta display names
				ItemMeta meta1 = item1.getItemMeta();
				ItemMeta meta2 = item2.getItemMeta();
				String name1 = meta1.hasDisplayName() ? meta1.getDisplayName() : "";
				String name2 = meta2.hasDisplayName() ? meta2.getDisplayName() : "";
				
				diff = name1.compareTo(name2);
				if ( diff != 0 )
					return diff;
				
				// then sort by item meta localized names
				name1 = meta1.hasLocalizedName() ? meta1.getLocalizedName() : "";
				name2 = meta2.hasLocalizedName() ? meta2.getLocalizedName() : "";
				
				diff = name1.compareTo(name2);
				if ( diff != 0 )
					return diff;
				
				// then sort by durability
				return item1.getDurability() - item2.getDurability();
			}
		});
		
		// the code below is because Inventory.setStorageContents() don't always works as described
		
		// use own inventory methods to fill item stacks (why reinvent the wheel?)
		Inventory vaporChest = getServer().createInventory(null, (size += 8) - (size % 9));
		for (ItemStack item : storage)
			// enhances: v1 some performance improvements (break loops timely)
			if ( isEmptyItem(item) )
				break;
			else
				vaporChest.addItem(item);
		
		// set storage contents
		System.arraycopy(vaporChest.getContents(), 0, contents, from, size - 8);
		chest.setContents(contents);
		
		// done, send updated inventory views to player
		e.setCancelled(true);
		player.updateInventory();
	}
	
	
	// utility method
	// fixes: v1 exception with items in 2nd half large chests (by adding legacy air)
	private boolean isEmptyItem(ItemStack item)
	{
		return item == null || item.getType() == Material.AIR || item.getType() == Material.LEGACY_AIR;
	}
	
}
