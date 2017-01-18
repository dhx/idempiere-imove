/**
 * 
 */
package at.dhx.adempiere.imove.callout;

import java.math.BigDecimal;
import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MProduct;
import org.compiere.model.MStorageOnHand;
import org.compiere.util.Env;

/**
 * @author dhx
 *
 */
public class CalloutFromFactory implements IColumnCallout {

	/* (non-Javadoc)
	 * @see org.adempiere.base.IColumnCallout#start(java.util.Properties, int, org.compiere.model.GridTab, org.compiere.model.GridField, java.lang.Object, java.lang.Object)
	 */
	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab,
			GridField mField, Object value, Object oldValue) {

		Integer M_Product_ID = (Integer)value;
		if (M_Product_ID == null || M_Product_ID.intValue() == 0)
			return "";

		Integer M_AttributeSetInstance_ID = 0;
		if (Env.getContextAsInt(ctx, WindowNo, Env.TAB_INFO, "M_Product_ID") == M_Product_ID.intValue()
				&& Env.getContextAsInt(ctx, WindowNo, Env.TAB_INFO, "M_AttributeSetInstance_ID") != 0)
				M_AttributeSetInstance_ID = Env.getContextAsInt(ctx, WindowNo, Env.TAB_INFO, "M_AttributeSetInstance_ID");

		int M_Locator_ID = Env.getContextAsInt(ctx, WindowNo, mTab.getTabNo(), "M_Locator_ID");
		BigDecimal movementQty = (BigDecimal) mTab.getValue("MovementQty");
		BigDecimal qtyOnHand = MStorageOnHand.getQtyOnHandForLocator(M_Product_ID, M_Locator_ID, M_AttributeSetInstance_ID, null);

		if (M_Locator_ID > 0) {
			if (qtyOnHand.compareTo(movementQty) < 0) {
				M_Locator_ID = 0;
			}
		}

		if (M_Locator_ID <= 0) {
			// No locator set, if a source warehouse is set on the movement try to find a locator with sufficient qtyonhand
			Integer warehouse_src_id = (Integer)mTab.getParentTab().getValue("M_WarehouseSource_ID");
			if (warehouse_src_id != null && warehouse_src_id > 0) {
				M_Locator_ID = MStorageOnHand.getM_Locator_ID(warehouse_src_id,
						M_Product_ID, M_AttributeSetInstance_ID, movementQty, null);
				if (M_Locator_ID > 0) {
					mTab.setValue("M_Locator_ID",M_Locator_ID);
				}
			}
		}

		// If a destination warehouse is set search for any storage on hand entry
		// for this product (even with negative qty) and get the locator with the highest qty on hand
		Integer warehouse_dest_id = (Integer)mTab.getParentTab().getValue("M_Warehouse_ID");
		if (warehouse_dest_id != null && warehouse_dest_id > 0) {
			Integer M_LocatorTo_ID = MStorageOnHand.getM_Locator_ID(warehouse_dest_id,
					M_Product_ID, M_AttributeSetInstance_ID, new BigDecimal("-9999999999999"), null);
			if (M_LocatorTo_ID != null && M_LocatorTo_ID > 0) {
				mTab.setValue("M_LocatorTo_ID",M_LocatorTo_ID);
			}
		}

		checkQtyAvailable(ctx, mTab, WindowNo, M_Product_ID, null);
		return null;
	}

	/**
	 * Check available qty (taken from CalloutMovement @author Jorg Janke, Teo Sarca)
	 * 
	 * @param ctx context
	 * @param mTab Model Tab
	 * @param WindowNo current Window No
	 * @param M_Product_ID product ID
	 * @param MovementQty movement qty (if null will be get from context "MovementQty")
	 */
	private void checkQtyAvailable(Properties ctx, GridTab mTab, int WindowNo, int M_Product_ID, BigDecimal MovementQty) {
		// Begin Armen 2006/10/01
		if (M_Product_ID != 0) {
			MProduct product = MProduct.get(ctx, M_Product_ID);
			if (product.isStocked()) {
				if (MovementQty == null)
					MovementQty = (BigDecimal) mTab.getValue("MovementQty");
				int M_Locator_ID = Env.getContextAsInt(ctx, WindowNo, mTab.getTabNo(), "M_Locator_ID");
				// If no locator, don't check anything and assume is ok
				if (M_Locator_ID <= 0)
					return;
				int M_AttributeSetInstance_ID = Env.getContextAsInt(ctx, WindowNo, mTab.getTabNo(), "M_AttributeSetInstance_ID");
				BigDecimal available = MStorageOnHand.getQtyOnHandForLocator(M_Product_ID, M_Locator_ID, M_AttributeSetInstance_ID, null);
				
				if (available == null)
					available = Env.ZERO;
				if (available.signum() == 0)
					mTab.fireDataStatusEEvent("NoQtyAvailable", "0", false);
				else if (available.compareTo(MovementQty) < 0)
					mTab.fireDataStatusEEvent("InsufficientQtyAvailable", available.toString(), false);
			}
		}
		// End Armen
	}
	
}
