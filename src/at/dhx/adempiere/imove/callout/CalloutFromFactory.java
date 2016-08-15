/**
 * 
 */
package at.dhx.adempiere.imove.callout;

import java.math.BigDecimal;
import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
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
		System.out.println(ctx + " - " + WindowNo + " - " + mTab + " - " + mField + " - " + value + " - " + oldValue);

		Integer M_Product_ID = (Integer)value;
		if (M_Product_ID == null || M_Product_ID.intValue() == 0)
			return "";

		Integer M_AttributeSetInstance_ID = 0;
		if (Env.getContextAsInt(ctx, WindowNo, Env.TAB_INFO, "M_Product_ID") == M_Product_ID.intValue()
				&& Env.getContextAsInt(ctx, WindowNo, Env.TAB_INFO, "M_AttributeSetInstance_ID") != 0)
				M_AttributeSetInstance_ID = Env.getContextAsInt(ctx, WindowNo, Env.TAB_INFO, "M_AttributeSetInstance_ID");
		
		BigDecimal Qty = (BigDecimal) mTab.getValue("MovementQty");
		
		int M_Locator_ID = Env.getContextAsInt(ctx, WindowNo, mTab.getTabNo(), "M_Locator_ID");
		if (M_Locator_ID <= 0) {
			// No locator set, if a source warehouse is set on the  a source
			Integer warehouse_src_id = (Integer)mTab.getParentTab().getValue("M_WarehouseSource_ID");
			if (warehouse_src_id > 0) {
				M_Locator_ID = MStorageOnHand.getM_Locator_ID(warehouse_src_id,
						M_Product_ID, M_AttributeSetInstance_ID, Qty, null);
				if (M_Locator_ID > 0) {
					mTab.setValue("M_Locator_ID",M_Locator_ID);
				}
			}
		}

		Integer M_LocatorTo_ID = Env.getContextAsInt(ctx, WindowNo, mTab.getTabNo(), "M_LocatorTo_ID");
		if (M_LocatorTo_ID <= 0) {
			// No locator set, if a destination warehouse is set search for any storage on hand entry
			// for this product (even with negative qty) and get the locator with the highest qty on hand
			Integer warehouse_dest_id = (Integer)mTab.getParentTab().getValue("M_Warehouse_ID");
			if (warehouse_dest_id > 0) {
				M_LocatorTo_ID = MStorageOnHand.getM_Locator_ID(warehouse_dest_id,
						M_Product_ID, M_AttributeSetInstance_ID, new BigDecimal("-9999999999999"), null);
				if (M_LocatorTo_ID > 0) {
					mTab.setValue("M_LocatorTo_ID",M_LocatorTo_ID);
				}
			}
		}

		return null;
	}

}
