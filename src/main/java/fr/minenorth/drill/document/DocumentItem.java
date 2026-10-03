package fr.minenorth.drill.document;
import net.minecraft.world.item.Item;
public class DocumentItem extends Item { private final String type; public DocumentItem(String type){super(new Item.Properties().stacksTo(1));this.type=type;} public String type(){return type;} }
