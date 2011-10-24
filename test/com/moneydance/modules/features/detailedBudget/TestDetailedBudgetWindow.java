package com.moneydance.modules.features.detailedBudget;

import com.moneydance.apps.md.model.BudgetItem;

import junit.framework.TestCase;

public class TestDetailedBudgetWindow extends TestCase
{
	public void testGetBudgetedAmount()
	{
	    long r;
		
		// Semi-monthly not-prorated, one complete, one partial.
		r = DetailedBudgetWindow.getBudgetedAmount(20100101, 20101231, 
												   BudgetItem.INTERVAL_ONCE_SEMI_MONTHLY, 100, 
												   20100601, 20100619);
		assertEquals(200, r); 
		
		// Semi-monthly not-prorated, two complete
		r = DetailedBudgetWindow.getBudgetedAmount(20100101, 20101231, 
												   BudgetItem.INTERVAL_ONCE_SEMI_MONTHLY, 100, 
												   20100601, 20100630);
		assertEquals(200, r); 
		
		// Semi-monthly not-prorated, two complete, one day into next
		r = DetailedBudgetWindow.getBudgetedAmount(20100101, 20101231, 
												   BudgetItem.INTERVAL_ONCE_SEMI_MONTHLY, 100, 
												   20100601, 20100701);
		assertEquals(300, r); 
		
		// Semi-monthly prorated, one complete, 4 days of next.
		r = DetailedBudgetWindow.getBudgetedAmount(20100101, 20101231, 
												   BudgetItem.INTERVAL_SEMI_MONTHLY, 100, 
												   20100601, 20100619);
		assertEquals(127, r); 

		// Semi-monthly prorated, two complete
		r = DetailedBudgetWindow.getBudgetedAmount(20100101, 20101231, 
												   BudgetItem.INTERVAL_SEMI_MONTHLY, 100, 
												   20100601, 20100630);
		assertEquals(200, r); 
		
		// Semi-monthly not-prorated, one complete, one partial because budget ends.
		r = DetailedBudgetWindow.getBudgetedAmount(20100101, 20100619, 
												   BudgetItem.INTERVAL_ONCE_SEMI_MONTHLY, 100, 
												   20100601, 20100630);
		assertEquals(200, r); 
		
		// Semi-monthly prorated, one complete, 4 days of next because budget ends.
		r = DetailedBudgetWindow.getBudgetedAmount(20100101, 20100619, 
												   BudgetItem.INTERVAL_SEMI_MONTHLY, 100, 
												   20100601, 20100630);
		assertEquals(127, r); 
		
		// Monthly non-prorated, one complete, 1 day of next
		r = DetailedBudgetWindow.getBudgetedAmount(20100101, 20101231, 
												   BudgetItem.INTERVAL_ONCE_MONTHLY, 100, 
												   20100601, 20100701);
		assertEquals(200, r); 

		// Monthly prorated, one complete, 1 day of next
		r = DetailedBudgetWindow.getBudgetedAmount(20100101, 20101231, 
												   BudgetItem.INTERVAL_MONTHLY, 100, 
												   20100601, 20100701);
		assertEquals(103, r); 

		// Monthly prorated, one partial
		r = DetailedBudgetWindow.getBudgetedAmount(20100101, 20101231, 
												   BudgetItem.INTERVAL_MONTHLY, 100, 
												   20100601, 20100629);
		assertEquals(97, r); 

		// Weekly prorated, two complete
		r = DetailedBudgetWindow.getBudgetedAmount(20100101, 20101231, 
												   BudgetItem.INTERVAL_WEEKLY, 100, 
												   20100601, 20100614);
		assertEquals(200, r); 

		// Weekly prorated, two complete plus 3 days
		r = DetailedBudgetWindow.getBudgetedAmount(20100101, 20101231, 
												   BudgetItem.INTERVAL_WEEKLY, 100, 
												   20100601, 20100617);
		assertEquals(243, r); 
		
		// Monthly not prorated, 
		r = DetailedBudgetWindow.getBudgetedAmount(20100101, 20100301, 
												   BudgetItem.INTERVAL_MONTHLY, 62800, 
												   20100101, 20100619);
		assertEquals(127626, r);

		// No repeat, within interval
		r = DetailedBudgetWindow.getBudgetedAmount(20100101, 20101231,
		                                           BudgetItem.INTERVAL_NO_REPEAT, 100,
		                                           20100601, 20100619);
		assertEquals(100, r);
	}
}
