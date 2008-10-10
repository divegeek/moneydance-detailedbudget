package com.moneydance.modules.features.detailedBudget;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;

import com.moneydance.apps.md.model.AbstractTxn;
import com.moneydance.apps.md.model.Account;
import com.moneydance.apps.md.model.Budget;
import com.moneydance.apps.md.model.BudgetItem;
import com.moneydance.apps.md.model.BudgetList;
import com.moneydance.apps.md.model.ExpenseAccount;
import com.moneydance.apps.md.model.IncomeAccount;
import com.moneydance.apps.md.model.TransactionSet;
import com.moneydance.apps.md.model.TxnTagSet;
import com.moneydance.awt.AwtUtil;

/** Detailed Budget.
 * Can include subtotals per week, month, year.
 * With subtotals it will always show Actual Amount. You can
 * include the budgeted Amount with that, and the difference.
 * Does NOT give budgeted amount for an item if not in
 * the budgeted period (ie does not do what MoneyDance Budget report, 
 * add per year and divide per period).
 * */
public class DetailedBudgetWindow extends JFrame {

    private Main extension;
	private String budgetName;
	private String budgetPeriod;
	private Date startDate;
	private Date endDate;
	private String subTotalBy;
	private boolean budgetWithSubtotal;
	private boolean diffWithSubtotal;
	private boolean showAllAccounts;
	
	private JEditorPane txtReport;
	private JButton printButton;
	private JButton saveButton;
	private JButton closeButton;

	/** Categories to shoiw in report*/
	private List categories = null;
	
	public static final DecimalFormat CURR_FMT = new DecimalFormat("#,##0");
	public static final DecimalFormat CENTS_FMT = new DecimalFormat("00");
	public static final SimpleDateFormat DT_FMT = new SimpleDateFormat("yyyy-MM-dd");
	
	public static final int START_DAY_OF_WEEK = Calendar.MONDAY;
	
	public static final int INCOME_ACCOUNTS = 0;
	public static final int EXPENSE_ACCOUNTS = 1;
	public static final int DIFF_ACCOUNTS = 2; // Income - Expense
	
	// -------------------------------------------
	
	/**
	 * Detailed Budget Report
	 * @param budgetName
	 * @param budgetPeriod
	 * @param startDate
	 * @param endDate
	 * @param subTotalBy
	 * @param budgetWithSubtotal
	 * @param diffWithSubtotal
	 */
	public DetailedBudgetWindow(Main extension, String budgetName, String budgetPeriod,
			Date startDate, Date endDate, String subTotalBy,
			boolean budgetWithSubtotal, boolean diffWithSubtotal,
			boolean showAllAccounts) {
	    super("Detailed Budget");
//	    System.out.println("Detailed Budget");
	    this.extension = extension;
	    this.budgetName = budgetName;
	    this.budgetPeriod = budgetPeriod;
	    this.startDate = startDate;
	    this.endDate = endDate;
	    this.subTotalBy = subTotalBy;
	    this.budgetWithSubtotal = budgetWithSubtotal;
	    this.diffWithSubtotal = diffWithSubtotal;
	    this.showAllAccounts = showAllAccounts;

	    // Get a list of all categories
		categories = getCategories();

	    JPanel p = new JPanel(new GridBagLayout());
	    p.setBorder(new EmptyBorder(10,10,10,10));

	    // Text Area
	    txtReport = new JEditorPane();
	    txtReport.setEditable(false);
	    txtReport.setContentType("text/html");
	    txtReport.setText(getReportStr());
	    p.add(new JScrollPane(txtReport), AwtUtil.getConstraints(0,0,1,1,4,1,true,true));
	    p.add(Box.createVerticalStrut(8), AwtUtil.getConstraints(0,2,0,0,1,1,false,false));
	    printButton = new JButton("Print");
	    printButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				print();
			}
		});
	    p.add(printButton, AwtUtil.getConstraints(0,3,1,0,1,1,false,true));
	    saveButton = new JButton("Save");
	    saveButton.addActionListener(new ActionListener() {
	    	public void actionPerformed(ActionEvent e) {
	    		save();
	    	}
	    });
	    p.add(saveButton, AwtUtil.getConstraints(1,3,1,0,1,1,false,true));
	    closeButton = new JButton("Close");
	    closeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				close();
			}
		});
	    p.add(closeButton, AwtUtil.getConstraints(2,3,1,0,1,1,false,true));
	    
	    getContentPane().add(p);

	    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	    enableEvents(WindowEvent.WINDOW_CLOSING);

	    setSize(600, 500);
	    AwtUtil.centerWindow(this);

//		System.out.println("Done Init DB.");
	}

	/**
	 * Print Report
	 */
	protected void print() {
		DocumentRenderer dr = new DocumentRenderer();
		dr.print(txtReport);
	}

	/** Get the Report based on parameters given.
	 * Will be as HTML format. */
	private String getReportStr() {
//		System.out.println("getReportStr");
		StringBuffer sb = new StringBuffer();
		// Start
		sb.append("<HTML>");
		// Heading
		sb.append("<font size=5><strong>Detailed Budget Report</strong></font><br><br>");
		sb.append("<font size=4><strong>").append(getBudgetPeriodStr());
		sb.append("</strong></font><br>");
		if (!subTotalBy.equals("None")) {
			sb.append("<font size=4><strong>");
			sb.append("Subtotal by ").append(subTotalBy);
			sb.append("</strong></font><br>");
		}
		SimpleDateFormat pdf = new SimpleDateFormat("d MMM yyyy");
		sb.append("Date: <strong>").append(pdf.format(new Date())).append("</strong><br>");
		sb.append("Budget: <strong>").append(budgetName).append("</strong><br>");
		sb.append("Period: <strong>").append(pdf.format(startDate)).
			append("</strong> to <strong>").append(pdf.format(endDate)).append("</strong><br><br>");
		// Body
		
		// Get Subtotal times
		List columns = getDetailedBudgetColumns(startDate,endDate);
		
		// Fill Actual and Budgeted Amounts for each column
		for (Iterator iterator = columns.iterator(); iterator.hasNext();) {
			DetailedBudgetColumn col = (DetailedBudgetColumn) iterator.next();
			
			// Map of int (account number) to DetailedBudgetItem
			col.detIncomeItems =  getDetailedBudgetItems(col.startDay, col.endDay, INCOME_ACCOUNTS);
			col.detExpenseItems =  getDetailedBudgetItems(col.startDay, col.endDay, EXPENSE_ACCOUNTS);
		}
		
		// Number of table columns in subtotals
		int numSubTotalCols = getNumSubtotalsColumns();
		
		sb.append("<table border=\"1\">\n");
		// First line of header
		sb.append("<tr><td align=\"center\"><strong>Item</strong></td>");
		if (columns.size() > 1) {
			for (Iterator iterator = columns.iterator(); iterator.hasNext();) {
				DetailedBudgetColumn col = (DetailedBudgetColumn) iterator.next();
				sb.append("<td align=\"center\" colspan="+numSubTotalCols+"><strong>"+DT_FMT.format(col.startDay)+
						" - "+DT_FMT.format(col.endDay)+"</strong></td>");
			}
			sb.append("<td align=\"center\" colspan=3><strong>TOTAL</strong></td>");
			sb.append("</tr>\n");
			// Second line of header
			sb.append("<tr><td>&nbsp</td>\n");
			if (columns.size() > 1) {
				for (Iterator iterator = columns.iterator(); iterator.hasNext();) {
					DetailedBudgetColumn col = (DetailedBudgetColumn) iterator.next();
					if (budgetWithSubtotal || columns.size() == 1) {
						sb.append("<td align=\"center\"><strong>Budget</strong></td>");
					}
					sb.append("<td align=\"center\"><strong>Actual</strong></td>");
					if (diffWithSubtotal || columns.size() == 1) {
						sb.append("<td align=\"center\"><strong>Diff</strong></td>");
					}
				}
			}
		}
		
		sb.append("<td align=\"center\"><strong>Budget</strong></td>");
		sb.append("<td align=\"center\"><strong>Actual</strong></td>");
		sb.append("<td align=\"center\"><strong>Diff</strong></td>");
		sb.append("</tr>\n");

		sb.append(getCategoriesHTML(columns,INCOME_ACCOUNTS));
		sb.append(getCategoriesHTML(columns,EXPENSE_ACCOUNTS));
		sb.append("<tr><td colspan="+getNumTableColumns(columns)+">&nbsp;</td></tr>\n");
		
		sb.append(getCategoriesTotalHTML(columns,INCOME_ACCOUNTS));
		sb.append(getCategoriesTotalHTML(columns,EXPENSE_ACCOUNTS));
		sb.append(getCategoriesTotalHTML(columns,DIFF_ACCOUNTS));
		
		sb.append("</table>");
		
		// End
		sb.append("</HTML>");
//		System.out.println("Done getReportStr.");
		return sb.toString();
	}
	
	private String getBudgetPeriodStr() {
		Date now = startDate;
		SimpleDateFormat MONTH_DF = new SimpleDateFormat("MMMM yyyy");
		SimpleDateFormat YEAR_DF = new SimpleDateFormat("yyyy");
		if (budgetPeriod.equals("Month to Date")) return MONTH_DF.format(now) + " to Date";
		if (budgetPeriod.equals("Quarter to Date")) return YEAR_DF.format(now) + " Quarter " + DateUtil.getQuarterNum(now) + " to Date";
		if (budgetPeriod.equals("Year to Date")) return YEAR_DF.format(now) + " to Date";
		if (budgetPeriod.equals("This Month")) return MONTH_DF.format(now);
		if (budgetPeriod.equals("This Quarter")) return YEAR_DF.format(now) + " Quarter " + DateUtil.getQuarterNum(now);
		if (budgetPeriod.equals("This Year")) return YEAR_DF.format(now);
		if (budgetPeriod.equals("Last Month")) return MONTH_DF.format(now);
		if (budgetPeriod.equals("Last Quarter")) return YEAR_DF.format(now) + " Quarter " + DateUtil.getQuarterNum(now);
		if (budgetPeriod.equals("Last Year")) return YEAR_DF.format(now);
		if (budgetPeriod.equals("Custom")) return "Custom";
		
		return "";
	}

	/**
	 * Number of Report Columns for each subtotal
	 * @return
	 */
	public int getNumSubtotalsColumns() {
		int numSubTotalCols = 1;
		if (budgetWithSubtotal) numSubTotalCols++;
		if (diffWithSubtotal) numSubTotalCols++;
		return numSubTotalCols;
	}
	
	/**
	 * Total number of columns for report row
	 * @param columns
	 * @return
	 */
	public int getNumTableColumns(List columns) {
		int i = 4;
		if (columns.size() > 1) {
			i += getNumSubtotalsColumns() * columns.size();
		}
		return i;
	}

	/** Categories HTML for Income or Expenses */
	public String getCategoriesHTML(List columns, int type) {
		StringBuffer sbo = new StringBuffer();
		sbo.append("<tr><td colspan="+getNumTableColumns(columns)+"><strong>");
		if (type == INCOME_ACCOUNTS) sbo.append("INCOME");
		else if (type == EXPENSE_ACCOUNTS) sbo.append("EXPENSE");
		sbo.append("</strong></td></tr>");
		
		// Categories
		for (Iterator iterator = categories.iterator(); iterator.hasNext();) {
			Account account = (Account) iterator.next();
			StringBuffer sb = new StringBuffer();
			if (account == null) continue;
			// Only accept income or expense accounts
			if (type == INCOME_ACCOUNTS && !(account instanceof IncomeAccount)) continue;
			else if (type == EXPENSE_ACCOUNTS && !(account instanceof ExpenseAccount)) continue;
//			System.out.println("  account="+account.getAccountName()+" "+account);

			Integer accNum = new Integer(account.getAccountNum());
			// Account Name
			sb.append("<tr><td>");
			sb.append(getAccountName(account));
			sb.append("</td>");
			
			// Columns
			long totalActual = 0;
			long totalBudget = 0;
			for (Iterator iterator2 = columns.iterator(); iterator2.hasNext();) {
				DetailedBudgetColumn col = (DetailedBudgetColumn) iterator2.next();
				DetailedBudgetItem item = null;
				if (type == INCOME_ACCOUNTS) item = (DetailedBudgetItem) col.detIncomeItems.get(accNum);
				else if (type == EXPENSE_ACCOUNTS) item = (DetailedBudgetItem) col.detExpenseItems.get(accNum);
				long actual = 0;
				long budget = 0;
				if (item != null) {
					actual = item.actualAmount;
					budget = item.budgetAmount;
				}
//				System.out.println(" -- item="+item+" actual="+actual+" budget="+budget);
				
				if (budgetWithSubtotal || columns.size() == 1) {
					sb.append("<td align=\"right\">").append(getCurrencyStr(budget,null)).append("</td>");
				}
				sb.append("<td align=\"right\">").append(getCurrencyStr(actual,col.endDay)).append("</td>");
				if (diffWithSubtotal || columns.size() == 1) {
					long diff = budget - actual;
					if (type == INCOME_ACCOUNTS) {
						diff = actual - budget;
					}
					sb.append("<td align=\"right\">").append(getCurrencyStr(diff,col.endDay)).append("</td>");
				}
				totalActual += actual;
				totalBudget += budget;
			}
			
			// If more than 1 column add a total column
			if (columns.size() > 1) {
				sb.append("<td align=\"right\">").append(getCurrencyStr(totalBudget,null)).append("</td>");
				sb.append("<td align=\"right\">").append(getCurrencyStr(totalActual,null)).append("</td>");
				long diff = totalBudget - totalActual;
				if (type == INCOME_ACCOUNTS) {
					diff = totalActual - totalBudget;
				}
				sb.append("<td align=\"right\">").append(getCurrencyStr(diff,null)).append("</td>");
			}
			sb.append("</tr>\n");
			
			// Do we show all accounts, even if all 0?
			if (showAllAccounts || totalActual != 0 || totalBudget != 0) {
				sbo.append(sb);
			}
		}
	
		return sbo.toString();
	}
	
	/** Categories HTML for TOTAL of Income or Expenses */
	public String getCategoriesTotalHTML(List columns, int type) {
		StringBuffer sb = new StringBuffer();
		// TOTALS
		sb.append("<tr><td><strong>");
		if (type == INCOME_ACCOUNTS) sb.append("TOTAL INCOME");
		else if (type == EXPENSE_ACCOUNTS) sb.append("TOTAL EXPENSE");
		else if (type == DIFF_ACCOUNTS) sb.append("TOTAL DIFF");
		sb.append("</strong></td>");
		
		// Columns
		long totalActual = 0;
		long totalBudget = 0;
		for (Iterator iterator2 = columns.iterator(); iterator2.hasNext();) {
			DetailedBudgetColumn col = (DetailedBudgetColumn) iterator2.next();
			long actual = 0;
			long budget = 0;
			if (type == INCOME_ACCOUNTS) {
				actual = col.getTotalIncomeActualAmount();
				budget = col.getTotalIncomeBudgetAmount();
			}
			else if (type == EXPENSE_ACCOUNTS) {
				actual = col.getTotalExpenseActualAmount();
				budget = col.getTotalExpenseBudgetAmount();
			}
			else if (type == DIFF_ACCOUNTS) {
				actual = col.getTotalIncomeActualAmount() - col.getTotalExpenseActualAmount();
				budget = col.getTotalIncomeBudgetAmount() - col.getTotalExpenseBudgetAmount();
			}
			
			if (budgetWithSubtotal || columns.size() == 1) {
				sb.append("<td align=\"right\"><strong>").append(getCurrencyStr(budget,null)).append("</strong></td>");
			}
			sb.append("<td align=\"right\"><strong>").append(getCurrencyStr(actual,col.endDay)).append("</strong></td>");
			if (diffWithSubtotal || columns.size() == 1) {
				sb.append("<td align=\"right\"><strong>").append(getCurrencyStr(budget - actual,col.endDay)).append("</strong></td>");
			}
			totalActual += actual;
			totalBudget += budget;
		}
		
		// If more than 1 column add a total column
		if (columns.size() > 1) {
			sb.append("<td align=\"right\"><strong>").append(getCurrencyStr(totalBudget,null)).append("</strong></td>");
			sb.append("<td align=\"right\"><strong>").append(getCurrencyStr(totalActual,null)).append("</strong></td>");
			sb.append("<td align=\"right\"><strong>").append(getCurrencyStr(totalBudget - totalActual,null)).append("</strong></td>");
		}
		sb.append("</tr>\n");
		
		return sb.toString();
	}
	
	/**
	 * Get a DetailedBudgetColumn object which contains all Actual and Budgeted Values
	 * for a given time period.
	 * @param startDay Start Day of Period
	 * @param endDay End Day of Period
	 * @return
	 */
	private List getDetailedBudgetColumns(Date startDay, Date endDay) {
		List columns = new ArrayList();
		
		Date sd = startDay;

		// No Subtotals
		if (subTotalBy == null || subTotalBy.equals("None")) {
			DetailedBudgetColumn col = new DetailedBudgetColumn(startDay,endDay);
			columns.add(col);
		} 
		// Want subtotals
		else  {
			boolean done = false;
			while (!done) {
				Date e2 = null;
				if (subTotalBy.equals("Week")) {
					e2 = DateUtil.getEndOfWeek(sd,START_DAY_OF_WEEK);
				}
				else if (subTotalBy.equals("Month")) {
					e2 = DateUtil.getEndOfMonth(sd);
				}
				else if (subTotalBy.equals("Year")) {
					e2 = DateUtil.getEndOfYear(sd);
				} 
				else {
					// Shouldnt get here
					break;
				}
				// Have we reached the end
				if (DateUtil.isInSameDayOrAfter(e2, endDay)) {
					DetailedBudgetColumn col = new DetailedBudgetColumn(sd,endDay);
					columns.add(col);
					break;
				}
				// Next day
				DetailedBudgetColumn col = new DetailedBudgetColumn(sd,e2);
				columns.add(col);
				sd = DateUtil.setTimeZero(DateUtil.addDays(e2, 1));
			}
		}

		return columns;
	}

	/**
	 * Name of the account (Category)
	 * @param account
	 * @return
	 */
	private String getAccountName(Account account) {
		if (account == null) return "";
		StringBuffer sb = new StringBuffer();
		String[] names = account.getAllAccountNames();
		for (int i = 0; i < names.length; i++) {
			if (i > 0) sb.append(":");
			sb.append(names[i]);
		}
		
		return sb.toString();
	}

	/** Return amount as dollars and cents in HTML format. 
	 * If date in future return empty space.*/
	public String getCurrencyStr(long amount, Date dt) {
		StringBuffer sb = new StringBuffer();
		if (dt != null && dt.after(new Date()) && amount == 0) {
			return "&nbsp;";
		}
		if (amount < 0) sb.append("<font color=\"red\">");
		sb.append(CURR_FMT.format(amount/100)).append(".").append(CENTS_FMT.format(Math.abs(amount%100)));
		if (amount < 0) sb.append("</font>");
		return sb.toString();
	}
	
	/**
	 * Get a the budgeted and actual amounts for the period given.
	 * @param type Either INCOME_ACCOUNTS(0) or EXPENSE_ACCOUNTS(1)
	 * @return Map of int (account number) to DetailedBudgetItem
	 */
	private Map getDetailedBudgetItems(Date startDay, Date endDay, int type) {
//		System.out.println("getDetailedBudgetItems type="+type);
		Map txnMap = new HashMap();
		
		TransactionSet txSet = extension.getUnprotectedContext().getRootAccount().getTransactionSet();
		
		// Loop through all transactions
		Enumeration e = txSet.getAllTransactions();
		for (; e.hasMoreElements(); ) {
			AbstractTxn t = (AbstractTxn)e.nextElement();
			if (t == null) continue;
//			System.out.println("..txn="+t.getAccount().getAccountName()+" => "+t.getAccount().getClass().getName());
			// Only accept income or expense accounts
			if (type == INCOME_ACCOUNTS && !(t.getAccount() instanceof IncomeAccount)) continue;
			else if (type == EXPENSE_ACCOUNTS && !(t.getAccount() instanceof ExpenseAccount)) continue;
			
			// Is this transaction in the range?
			Date dt = new Date(t.getDate());
			if (DateUtil.isInRange(dt,startDay,endDay)) {
//				System.out.println("    in date range t="+t+" class="+t.getClass().getName());
				addTransaction(txnMap, t);
			}
		}

		// Loop through all budgeted items
		// If mopre than one budget has the same Category budgeted, it
		//will use the last budgets value (Should be same really anyway)
		BudgetList budList = extension.getUnprotectedContext().getRootAccount().getBudgetList();
		for (int i = 0; i < budList.getBudgetCount(); i++) {
			Budget b = budList.getBudget(i);
			for (int j = 0; j < b.getItemCount(); j++) {
				BudgetItem bi = b.getItem(j);
				Account a = bi.getTransferAccount();
				// Only accept income or expense accounts
				if (type == INCOME_ACCOUNTS && !(a instanceof IncomeAccount)) continue;
				else if (type == EXPENSE_ACCOUNTS && !(a instanceof ExpenseAccount)) continue;
				
				// Is the budget item scheduled for the given time period
				long budgetedAmount = getBudgetedAmount(bi,startDay,endDay);
				if (budgetedAmount == 0) continue;
				
				Integer accNum = new Integer(a.getAccountNum());
				DetailedBudgetItem item = (DetailedBudgetItem) txnMap.get(accNum);
				if (item == null) {
					item = new DetailedBudgetItem(accNum.intValue(),budgetedAmount,0);
					txnMap.put(accNum,item);
				} else {
					item.budgetAmount = budgetedAmount;
				}
			}
		}
		
		return txnMap;
	}

	/**
	 * What is the budgeted amount for the given time period
	 * @param bi
	 * @param startDay
	 * @param endDay
	 * @return
	 */
	private long getBudgetedAmount(BudgetItem bi, Date startDay, Date endDay) {
		Date budStartDate = DateUtil.getDateYYYYMMDD(bi.getIntervalStartDate());
		Date budEndDate = DateUtil.getDateYYYYMMDD(bi.getIntervalEndDate());
		Date budDt = budStartDate;
		
		// Is it after end day?
		if (budStartDate == null || budStartDate.after(endDay)) return 0;
		// Is it before start day
		if (budEndDate != null && budEndDate.before(startDay)) return 0;
		
		long amount = 0;
		boolean done = false;
		while (!done) {
			// Is budget in range
			if (DateUtil.isInRange(budDt, startDay, endDay)) {
				amount += bi.getAmount();
			}
			// Get next budgeted date
			switch (bi.getInterval()) {
				case BudgetItem.INTERVAL_ANNUALLY: budDt = DateUtil.addYears(budDt, 1); break;
				case BudgetItem.INTERVAL_BI_MONTHLY: budDt = DateUtil.addMonths(budDt, 2); break;
				case BudgetItem.INTERVAL_BI_WEEKLY: budDt = DateUtil.addWeeks(budDt,2); break;
				case BudgetItem.INTERVAL_DAILY:budDt = DateUtil.addDays(budDt,1);break;
				case BudgetItem.INTERVAL_MONTHLY:budDt = DateUtil.addMonths(budDt,1);break;
				case BudgetItem.INTERVAL_SEMI_ANNUALLY: {
					if (DateUtil.isSameDayOfYear(budDt,budStartDate)) {
						budDt = DateUtil.addDays(budDt,182);
					} else {
						budDt = DateUtil.addDays(budDt,-182);
						budDt =DateUtil.addYears(budDt, 1);
					}
					break;
				}
				case BudgetItem.INTERVAL_SEMI_MONTHLY: {
					if (DateUtil.isSameDayOfMonth(budDt,budStartDate)) {
						budDt = DateUtil.addDays(budDt,15);
					} else {
						budDt = DateUtil.addDays(budDt,-15);
						budDt =DateUtil.addMonths(budDt, 1);
					}
					break;
				}
				case BudgetItem.INTERVAL_TRI_MONTHLY: budDt = DateUtil.addMonths(budDt, 3); break;
				case BudgetItem.INTERVAL_TRI_WEEKLY: budDt = DateUtil.addWeeks(budDt,3); break;
				case BudgetItem.INTERVAL_WEEKLY: budDt = DateUtil.addWeeks(budDt,1); break;
				default: done = true;
			}
			
			// Is it past date
			if (budDt.after(endDay) || budDt.after(budEndDate)) {
				break;
			}
		}
		
		return amount;
	}

	/**
	 * Add a transaction to the Map
	 * @param txnMap
	 * @param t
	 */
	private void addTransaction(Map txnMap, AbstractTxn t) {
		if (t == null) return;

		// Get txn account
		Account a = t.getAccount();
		long amount = t.getValue();
		if (t.getAccount() instanceof IncomeAccount) amount = -amount;
		// Get current actual amount
		Integer accNum = new Integer(a.getAccountNum());
		DetailedBudgetItem item = (DetailedBudgetItem) txnMap.get(accNum);
		if (item == null) {
			item = new DetailedBudgetItem(accNum.intValue(),0,amount);
			txnMap.put(accNum,item);
		} else {
			item.actualAmount += amount;
		}
	}

	/** Get all categories (A category is actually an Account object) based on 
	 * Budget selected */
	private List getCategories() {
		List categories = new ArrayList();
		
		// Do we get all the categories?
		if (!budgetName.equals("ALL")) {
			// Get only specified budget
			BudgetList budList = extension.getUnprotectedContext().getRootAccount().getBudgetList();
			Budget b = null;
			for (int i = 0; i < budList.getBudgetCount(); i++) {
				b = budList.getBudget(i);
				if (b.getName().equals(budgetName)) break;
			}
			// Found Budget, now just get categories for this budget
			if (b != null) {
				for (int j = 0; j < b.getItemCount(); j++) {
					BudgetItem bi = b.getItem(j);
					Account a = bi.getTransferAccount();
					addAccountAndSubaccounts(categories,a);
				}
				
				return categories;
			}

		}
		// Get all categories
		try {
			Enumeration sa = extension.getUnprotectedContext().getRootAccount().getSubAccounts();
			for (; sa.hasMoreElements(); ) {
				Account a = (Account)sa.nextElement();
				addAccountAndSubaccounts(categories,a);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return categories;
	}

	/** Add an account to the list if it isnt there already */
	private void addAccountAndSubaccounts(List categories, Account account) {
		if (account == null) return;
		if (!(account instanceof ExpenseAccount) && 
			!(account instanceof IncomeAccount)) return;
		
		// If not in list, add it
		if (!categories.contains(account)) {
			categories.add(account);
		}
		
		Enumeration ssa = account.getSubAccounts();
		for (; ssa.hasMoreElements(); ) {
			Account suba = (Account)ssa.nextElement();
			if (suba == null) continue;
			addAccountAndSubaccounts(categories, suba);
		}
		
	}

	/** Save the Report */
	protected void save() {
		//Create a file chooser
		JFileChooser fc = new JFileChooser();
		File defFile = new File(getBudgetPeriodStr()+".html");
		fc.setSelectedFile(defFile);
		fc.setFileFilter(new FileFilter() {
			public String getDescription() {
				return "HTML Files";
			}
		
			public boolean accept(File f) {
				if (f.getName().toLowerCase().endsWith("html")) return true;
				return false;
			}
		});
		
		//In response to a button click:
		int returnVal = fc.showSaveDialog(this);
		
		if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            
            try {
                BufferedWriter out = new BufferedWriter(new FileWriter(file));
                out.write(txtReport.getText());
                out.close();
            } catch (IOException e) {
            }
        }
	}

	/** Close Window */
	protected void close() {
		this.setVisible(false);
		this.dispose();
	}
	
	/**
	 * Budget Item contains account (category) and actual, budget amounts for time period. 
	 */
	class DetailedBudgetItem {
		int accountNum = 0;
		long budgetAmount = 0;
		long actualAmount = 0;
		
		public DetailedBudgetItem(int accountNum, long budgetAmount, long actualAmount) {
			this.accountNum = accountNum;
			this.budgetAmount = budgetAmount;
			this.actualAmount = actualAmount;
		}
	}

	/** Represents a subtotal column in the report including all income and
	 * expense categories.
	 * @author rolf
	 *
	 */ 
	class DetailedBudgetColumn {
		Date startDay = null;
		Date endDay = null;
		// Map of account (int) to DetailedBudgetItem for income accounts
		Map detIncomeItems = null;
		// Map of account (int) to DetailedBudgetItem for expense accounts
		Map detExpenseItems = null;
		
		public DetailedBudgetColumn(Date startDay, Date endDay) {
			this.startDay = startDay;
			this.endDay = endDay;
		}
		
		/** Total Budget amount for Income Accounts */
		public long getTotalIncomeBudgetAmount() {
			long total = 0;
			for (Iterator iterator = categories.iterator(); iterator.hasNext();) {
				Account account = (Account) iterator.next();
				Integer accNum = new Integer(account.getAccountNum());

				DetailedBudgetItem item = (DetailedBudgetItem)detIncomeItems.get(accNum);
				if (item != null) total += item.budgetAmount;
			}
			return total;
		}
		
		/** Total Actual amount for Income Accounts */
		public long getTotalIncomeActualAmount() {
			long total = 0;
			for (Iterator iterator = categories.iterator(); iterator.hasNext();) {
				Account account = (Account) iterator.next();
				Integer accNum = new Integer(account.getAccountNum());

				DetailedBudgetItem item = (DetailedBudgetItem)detIncomeItems.get(accNum);
				if (item != null) total += item.actualAmount;
			}
			return total;
		}
		
		/** Total Budget amount for Expense Accounts */
		public long getTotalExpenseBudgetAmount() {
			long total = 0;
			for (Iterator iterator = categories.iterator(); iterator.hasNext();) {
				Account account = (Account) iterator.next();
				Integer accNum = new Integer(account.getAccountNum());

				DetailedBudgetItem item = (DetailedBudgetItem)detExpenseItems.get(accNum);
				if (item != null) total += item.budgetAmount;
			}
			return total;
		}
		
		/** Total Actual amount for Expense Accounts */
		public long getTotalExpenseActualAmount() {
			long total = 0;
			for (Iterator iterator = categories.iterator(); iterator.hasNext();) {
				Account account = (Account) iterator.next();
				Integer accNum = new Integer(account.getAccountNum());

				DetailedBudgetItem item = (DetailedBudgetItem)detExpenseItems.get(accNum);
				if (item != null) total += item.actualAmount;
			}
			return total;
		}
		
	}
}
