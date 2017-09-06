/**
 * 
 */
package at.dhx.adempiere.imove.callout;

import java.math.BigDecimal;
import java.util.List;
import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MLocator;
import org.compiere.model.MProduct;
import org.compiere.model.MStorageOnHand;
import org.compiere.model.Query;
import org.compiere.util.Env;

/**
 * @author dhx
 *
 */
public class CalloutMovementLineDescription implements IColumnCallout {

	/* (non-Javadoc)
	 * @see org.adempiere.base.IColumnCallout#start(java.util.Properties, int, org.compiere.model.GridTab, org.compiere.model.GridField, java.lang.Object, java.lang.Object)
	 */
	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab,
			GridField mField, Object value, Object oldValue) {

		Integer warehouse_src_id = (Integer)mTab.getParentTab().getValue("M_WarehouseSource_ID");
		Integer warehouse_dest_id = (Integer)mTab.getParentTab().getValue("M_Warehouse_ID");
		
		String scan = (String) value;
		if (scan == null || scan.length() == 0) {
			return null;
		}
		
		if (scan.toUpperCase().equals("OK")) {
			// special OK scan code to finish this line
			mTab.setValue("Description","");
			Boolean success = mTab.dataNew(false);
			if (!success) {
				mField.setError(true);
				return "";
			}
			return null;
		}
		
		List<MProduct> products = MProduct.getByUPC(Env.getCtx(), scan, null);
		if (products.size() < 1) {
			
			if (warehouse_src_id != null && warehouse_src_id > 0) {
				int M_Locator_ID = findLocatorInWarehouse(scan, warehouse_src_id);
				if (M_Locator_ID > -1) {
					mTab.setValue("M_Locator_ID",M_Locator_ID);
					mTab.setValue("Description","");
					return null;
				}
			}
			
			if (warehouse_dest_id != null && warehouse_dest_id > 0) {
				int M_Locator_ID = findLocatorInWarehouse(scan, warehouse_dest_id);
				if (M_Locator_ID > -1) {
					mTab.setValue("M_LocatorTo_ID",M_Locator_ID);
					mTab.setValue("Description","");
					return null;
				}				
			}
			
			// when we do not find a locator or a product check if its a reasonable
			// qty
			try {
				BigDecimal scan_qty = new BigDecimal(scan);
				if (scan_qty.signum() > 0 && scan_qty.compareTo(new BigDecimal("1000")) < 0) {
					mTab.setValue("MovementQty",scan_qty);
					mTab.setValue("Description","");
					return null;						
				}
			} catch (NumberFormatException e) {
				// when its not a valid number just ignore it
			}				
			
		} else {
			mTab.setValue("M_Product_ID", products.get(0).getM_Product_ID());
			mTab.setValue("Description","");
		}
		return null;
	}

	protected int findLocatorId(String text)
	{
		if (text == null || text.length() == 0)
		{
			return 0;
		}
		
		if (text.endsWith("%"))
			text = text.toUpperCase();
		else
			text = text.toUpperCase() + "%";
		
		int M_Locator_ID = new Query(Env.getCtx(),
				MLocator.Table_Name,
				"UPPER(Value) LIKE ? ",
				null)
				.setClient_ID()
				.setOnlyActiveRecords(true)
				.setParameters(new Object[] { text })
				.firstId();
				
		return M_Locator_ID;
	}

	protected int findLocatorInWarehouse(String text, int M_Warehouse_ID)
	{
		if (text == null || text.length() == 0)
		{
			return 0;
		}
		
		if (text.endsWith("%"))
			text = text.toUpperCase();
		else
			text = text.toUpperCase() + "%";
		
		int M_Locator_ID = new Query(Env.getCtx(),
				MLocator.Table_Name,
				"UPPER(Value) LIKE ? AND M_Warehouse_ID=?",
				null)
				.setClient_ID()
				.setOnlyActiveRecords(true)
				.setParameters(new Object[] { text, M_Warehouse_ID })
				.firstId();
				
		return M_Locator_ID;
	}
	
}
