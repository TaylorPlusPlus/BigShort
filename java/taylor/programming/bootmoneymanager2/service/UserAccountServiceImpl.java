package taylor.programming.bootmoneymanager2.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import taylor.programming.bootmoneymanager2.model.PurchasedStock;
import taylor.programming.bootmoneymanager2.model.Stock;
import taylor.programming.bootmoneymanager2.model.UserAccount;
import taylor.programming.bootmoneymanager2.verification.ConfirmationToken;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

@Slf4j
@Service
public class UserAccountServiceImpl implements UserAccountService {

    HashMap<String, UserAccount> currentAccountMap = new HashMap<String, UserAccount>();

    @Autowired
    DataSource dataSource;

    private JdbcTemplate jt;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Getter
    @Value("${app.APIkey}")
    private String APIkey;
    @Getter
    @Value("${app.APIprefix}")
    private String APIprefix;


    @Override
    public UserAccount getCurrentAccount(String Username) {
        return currentAccountMap.get(Username);
    }

    public void setCurrentAccount(String username, UserAccount account) {
        currentAccountMap.put(username, account);
    }

    @Override
    public void removeCurrentAccount(String username) {
        currentAccountMap.remove(username);
    }


    /* =======================setCurrentPriceData method ===========================================================
        This method makes an API call to retrieve current stock information
        input: API information
        output: stock data
     */
    @Override
    public String showAll() {
        String returnString = "";

        for (String usernames : currentAccountMap.keySet()) {
            returnString += usernames + "  \n";
        }
        return returnString;
    }
    // ====================== END showAll method =======================================================================

    /* =======================saveUser method ===========================================================
        This method is used to save a user into the database. It calls other methods to validate the users
        input, and if it is all valid, is saves the user in the database. If the users input is not valid,
        it returns an integer based on which piece of information is not valid. This method does not check
        for a valid email because email validation is done prior to this method being called.
        input: userAccount
        output: int
     */
    @Override
    public int saveUser(UserAccount account) {

        //properties used within the method
        int everythingIsGood = 0;
        int usernameAlreadyExists = 1;
        int invalidInfo = 2;
        double startingAmount = 1000000;
        double amountInMargin = 1000000;
        String defaulImage = "null";


        //logging some data about the account
        log.info("username : {}", account.getUsername());
        log.info("passowrd : {}", account.getPassword());
        log.info("firstname: {}", account.getFirstName());
        log.info("lastName: {}", account.getLastName());
        log.info("email: {}", account.getEmail());

        //Username validation
        if (!checkValidUsername(account.getUsername())) {
            return invalidInfo;
        }
        //Password validation
        if (!checkValidPassword(account.getPassword())) {
            return invalidInfo;
        }
        //first and last name validation
        if (!checkValidFirstOrLastName(account.getFirstName()) || !checkValidFirstOrLastName(account.getLastName())) {
            return invalidInfo;
        }

        jt = new JdbcTemplate(dataSource);

        //Queries to insert the user into the database
        try {
            jt.execute("INSERT INTO MONEYMANAGERUSERS(username, password, enabled)"
                    + " VALUES('" + account.getUsername() + "' , '" + passwordEncoder.encode(account.getPassword()) + "' ,  'N' ) ");

            jt.execute(("INSERT INTO Authorities(username, authority)"
                    + " VALUES('" + account.getUsername() + "', 'ROLE_USER')"));

            jt.execute(("INSERT INTO UserAccountInformation"
                    + " VALUES('" + account.getUsername() + "','" + account.getFirstName() + "','"
                    + account.getLastName() + "','" + account.getEmail() + "','" + defaulImage + "',"
                    + startingAmount + "," + getNextBadgeCollectionId() + "," + getNextInvestmentCollectionId()
                    + ", " + amountInMargin + ")"));
        } catch (DataAccessException e) {
            log.info("{}", e);
            return usernameAlreadyExists;
        }

        return everythingIsGood;
    }
    // ====================== END saveUser method ======================================================================

    /* ============ checkValidUsername, checkValidPassword, checkValidFirstOrLastName, checkValidEmail methods =========
        Each of the following four methods are used to validate the input type corresponding to the method name. The
        input requirement for each of these methods can be found within each method declaration.
        inputs: Strings
        output: (true/false)/(is valid/ is Not valid)
     */
    @Override
    public boolean checkValidUsername(String username) {
        // username must contain only characters a-z, A-Z, 0-9
        // can contain only 1 underscore
        // must be less than or equal to 20 characters and greater than 3 characters
        // must contain at least 3 characters a-z or A-Z

        char evaluatedCharacter = ' ';
        int ascii;
        int characterCount = 0;
        int underScoreCount = 0;

        if (username.length() >= 20) {
            return false;
        }
        if (username.length() < 3) {
            return false;
        }
        for (int currentChar = 0; currentChar < username.length(); currentChar++) {

            evaluatedCharacter = username.charAt(currentChar);

            // converting the current character of the username to ascii
            ascii = (int) evaluatedCharacter;

            // valid char check
            if ((ascii >= 65 && ascii <= 90) || (ascii >= 97 && ascii <= 122) ||
                    (ascii >= 48 && ascii <= 57) || ascii == 95) {

                //Checking for character to see if username meets 3 non number character restriction
                if ((ascii >= 65 && ascii <= 90) || (ascii >= 97 && ascii <= 122)) {
                    characterCount++;
                }

                //Checking for _ to see if they only use 1 underscore
                if (ascii == 95) {
                    underScoreCount++;
                }
            } else {
                return false;
            }

        }

        if ((characterCount < 3) || (underScoreCount > 1)) {
            return false;
        }


        return true;
    }

    @Override
    public boolean checkValidPassword(String password) {
        // password must contain only characters a-z, A-Z, 0-9
        // can contain only 1 underscore
        // must be less than 50 characters and greater than 11 characters

        char evaluatedCharacter = ' ';
        int ascii;
        int underScoreCount = 0;

        //size checks
        if (password.length() > 50) {
            return false;
        }
        if (password.length() < 11) {
            return false;
        }

        //looping through each character
        for (int currentChar = 0; currentChar < password.length(); currentChar++) {

            evaluatedCharacter = password.charAt(currentChar);

            // converting the current character of the username to ascii
            ascii = (int) evaluatedCharacter;

            // valid char check
            if ((ascii >= 65 && ascii <= 90) || (ascii >= 97 && ascii <= 122) ||
                    (ascii >= 48 && ascii <= 57) || ascii == 95) {

                //Checking for _ to see if they only use 1 underscore
                if (ascii == 95) {
                    underScoreCount++;
                }
            } else {
                return false;
            }
        }

        //checking for max 1 underscore
        if ((underScoreCount > 1)) {
            return false;
        }
        return true;
    }

    @Override
    public boolean checkValidFirstOrLastName(String firstName) {
        // first or last name must contain only characters a-z, A-Z
        // must be less than 25 characters and greater than 2 characters
        // must contain at least 3 characters a-z or A-Z

        char evaluatedCharacter = ' ';
        int ascii;
        int characterCount = 0;
        int underScoreCount = 0;

        if (firstName.length() > 25) {
            return false;
        }
        if (firstName.length() < 3) {
            return false;
        }
        for (int currentChar = 0; currentChar < firstName.length(); currentChar++) {

            evaluatedCharacter = firstName.charAt(currentChar);

            // converting the current character of the username to ascii
            ascii = (int) evaluatedCharacter;

            // valid char check
            if ((ascii >= 65 && ascii <= 90) || (ascii >= 97 && ascii <= 122)) {

            } else {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean checkValidEmail(String email) {
        // email must contain only characters a-z, A-Z, 0-9, @ or .
        // can contain only 1 @
        // must be less than or equal to 25 characters and greater than 3 characters
        // must contain at least 3 characters a-z or A-Z

        char evaluatedCharacter = ' ';
        int ascii;
        int atCount = 0;


        if (email.length() >= 40) {
            log.info("Email failed: TOO LONG");
            return false;
        }
        if (email.length() <= 3) {
            log.info("Email failed: TOO SHORT");
            return false;
        }
        for (int currentChar = 0; currentChar < email.length(); currentChar++) {

            evaluatedCharacter = email.charAt(currentChar);

            // converting the current character of the username to ascii
            ascii = (int) evaluatedCharacter;

            // valid char check
            if ((ascii >= 65 && ascii <= 90) || (ascii >= 97 && ascii <= 122) ||
                    (ascii >= 48 && ascii <= 57) || (ascii == 46) || (ascii == 64)) {

                //keeping track of the @ symbol count
                if ((ascii == 64)) {
                    atCount++;
                }
            } else {
                log.info("Email failed: Invalid Character");
                return false;
            }
        }
        if (atCount > 1) {
            log.info("Email failed: TOO MANY @");
            return false;
        }

        // query for object to make sure the email isn't already in the database.

        String sql = "SELECT first_name FROM UserAccountInformation WHERE upper(email) = upper('" + email + "')";
        jt = new JdbcTemplate(dataSource);
        SqlRowSet set = jt.queryForRowSet(sql);
        ArrayList<String> nameList = new ArrayList<String>();
        nameList.clear();

        while (set.next()) {
            nameList.add(set.getString(1));
        }

        for(String name: nameList){
            log.info(" name = {}", name);
        }
        if (!nameList.isEmpty()) {
            log.info("Email failed: nameList is empty");
            return false;
        }
        return true;
    }

    //  ==========END checkValidUsername, checkValidPassword, checkValidFirstOrLastName, checkValidEmail methods =======


    /*  getNextBadgeCollectionId, getNextInvestmentCollectionId, getNextInvestmentId, getNextConfirmationTokenId methods

        Each of the following four methods are used query the database and determine the next ID for the type
         corresponding to the method name.
        inputs: null
        output: (int) id
     */
    @Override
    public int getNextBadgeCollectionId() {

        int currentBadgeCollectionMax = 0;
        String sql = "SELECT MAX(badge_collection_id) FROM UserAccountInformation";

        currentBadgeCollectionMax = jt.queryForObject(sql, Integer.class);
        currentBadgeCollectionMax++;

        return currentBadgeCollectionMax;
    }

    @Override
    public int getNextInvestmentCollectionId() {

        int currentInvestmentCollectionMax = 0;
        String sql = "SELECT MAX(investment_collection_id) FROM UserAccountInformation";

        currentInvestmentCollectionMax = jt.queryForObject(sql, Integer.class);
        currentInvestmentCollectionMax++;

        return currentInvestmentCollectionMax;
    }

    @Override
    public int getNextInvestmentId() {

        int currentInvestmentCollectionMax = 0;
        String sql = "SELECT MAX(investment_id) FROM Investment_Stock";

        currentInvestmentCollectionMax = jt.queryForObject(sql, Integer.class);
        currentInvestmentCollectionMax++;

        return currentInvestmentCollectionMax;
    }

    public int getNextConfirmationTokenId() {
        int currentTokenMax = 0;
        String sql = "SELECT MAX(token_id) FROM confirmation_token";

        currentTokenMax = jt.queryForObject(sql, Integer.class);
        currentTokenMax++;

        return currentTokenMax;
    }
    /*  =========== END getNextBadgeCollectionId, getNextInvestmentCollectionId,
                    getNextInvestmentId, getNextConfirmationTokenId methods ============================================
   */

    /*  =========== fillUserInfo method ================================================================================
        This method receives a username and then queries the database for the rest of the account information
        for that username, then fills the userAccount model out with that data.
        inputs: username
        output: sets the rest of the users data
     */
    @Override
    public void fillInUserInfo(String username) {

        String sql = "SELECT * FROM UserAccountInformation WHERE USERNAME = '" + username + "'";
        jt = new JdbcTemplate(dataSource);
        SqlRowSet set = jt.queryForRowSet(sql);

        while (set.next()) {

            currentAccountMap.get(username).setFirstName(set.getString(2));
            currentAccountMap.get(username).setLastName(set.getString(3));
            currentAccountMap.get(username).setEmail(set.getString(4));
            currentAccountMap.get(username).setImageId(set.getString(5));
            currentAccountMap.get(username).setAmountInSavings(set.getDouble(6));
            currentAccountMap.get(username).setBadge_collection_id(set.getInt(7));
            currentAccountMap.get(username).setInvestment_collection_id(set.getInt(8));
            currentAccountMap.get(username).setAmountInMargin(set.getDouble(9));
        }
    }
    // ===========END fillInUserInfo method ============================================================================


    /*  =========== processStock method ================================================================================
        This processes a stock purchase. This method ensures the user have enough funds to purchase the stock, if they
        do, it updates their stock collection as well as updates their account balances.
        input: user, stock to be purchased
        output: updated account information
     */
    @Override
    public boolean processStock(Stock stock, UserAccount user) {
        double totalCost = stock.numberOfShares * stock.close;
        int investmentId = getNextInvestmentId();
        String date = "";
        StringTokenizer tok = new StringTokenizer(stock.date, " ", false);
        if (tok.hasMoreTokens()) {
            date = tok.nextToken();
        }
        if (totalCost > user.getAmountInSavings()) {
            return false;
        } else {
            user.setAmountInSavings(user.getAmountInSavings() - totalCost);
            // update accountbalance in database
            jt.execute("UPDATE USERACCOUNTINFORMATION SET AMOUNT_IN_SAVINGS = " + user.getAmountInSavings()
                    + " WHERE username = '" + user.getUsername() + "'");

            //create an Investment_Stock instance of the stock.

            //create an investment_collection with collection id = users collection id number
            jt.execute("INSERT INTO Investment_Collection" +
                    " VALUES( " + user.getInvestment_collection_id() + " ," + investmentId + ")");

            // and investmentID that matches the investment_stock's id above.
            jt = new JdbcTemplate(dataSource);

            jt.execute("INSERT INTO Investment_Stock"
                    + " VALUES('" + investmentId + "' , '" + stock.getSymbol() + "' , "
                    + stock.getClose() + " ,'" + date + "' , " + stock.getNumberOfShares()
                    + ", " + user.getInvestment_collection_id() + ", 'NO')");
            PurchasedStock newStock = new PurchasedStock();
            newStock.setInvestmentID(investmentId);
            newStock.setSymbol(stock.getSymbol());
            newStock.setPurchase_price(stock.getClose());
            newStock.setNumberOfShares(stock.getNumberOfShares());
            newStock.setPurchaseDate(stock.getDate());
            currentAccountMap.get(user.getUsername()).getCurrentStockInvestments().add(newStock);
            return true;
        }

        //TODO NEED TO PROCESS ADDING THE STOCK TO THE ACCOUNT
    }
    // ===========END processStock method ============================================================================

    /* =========== processPurchasedStocks method =====================================================================
        This method is used to fill the current users stock portfolio. It uses the fillAllStocks method to retrieve
        a list of the users purchased stocks from the database and saves them locally within an array list.
        input: user
        output: true or false depending on whether the user has at least one stock
     */
    @Override
    public boolean processPurchasedStocks(UserAccount user) {
        boolean hasStocks = false;

        List<PurchasedStock> stocks = findAllStocks(user.getInvestment_collection_id());

        // logging some information about the user and their stocks
        log.info("purchasedStocks called, stock = : {}", stocks);
        log.info("purchasedStocks called, userId = : {}", user.getInvestment_collection_id());

        // filling the users stock array list with stocks from the database
        for (PurchasedStock stock : stocks) {
            hasStocks = true;

            user.getCurrentStockInvestments().add(stock);
        }
        return hasStocks;
    }
    // ========= END processPurchasedStocks method =====================================================================

    /* ========= findAllStocks method ==================================================================================
        This method is used to retrieve all of the stocks that a user has from the database.
        input: a users investmentCollectionId
        output: list of stocks
     */
    @Override
    public List<PurchasedStock> findAllStocks(int investmentCollectionId) {
        jt = new JdbcTemplate(dataSource);

        // query string
        String sql = "select * from Investment_stock where" +
                " Investment_collection_id = " + investmentCollectionId
                + " AND SOLD = 'NO'";

        // query execution
        List<PurchasedStock> returnList = jt.query(
                sql,
                (rs, rowNum) ->
                        new PurchasedStock(
                                rs.getInt("INVESTMENT_ID"),
                                rs.getString("SYMBOL"),
                                rs.getDouble("PURCHASE_PRICE"),
                                rs.getString("PURCHASE_DATE"),
                                rs.getDouble("NUMBER_OF_SHARES")
                        )
        );

        // logging each stock the user owns
        for (PurchasedStock stock : returnList) {
            log.info("stock: {}", stock);
        }
        return returnList;
    }
    //========= END findAllStocks method ===============================================================================

    /*========= shortStock method ==================================================================================
        This method is used to process a stock short. First, the users margin account balance is checked to ensure
        they have enough to borrow to lend the stock. If they do, the total price is deducted from their margin
        available and the stock is added to both their investment portfolio as well as their short portfolio.
        output: list of stocks
     */
    @Override
    public boolean shortStock(Stock stock, UserAccount user) {
        double totalCost = stock.numberOfShares * stock.close;
        int investmentId = getNextInvestmentId();
        String date = "";
        StringTokenizer tok = new StringTokenizer(stock.date, " ", false);
        if (tok.hasMoreTokens()) {
            date = tok.nextToken();
        }
        if (totalCost > user.getAmountInMargin()) {
            return false;
        } else {
            user.setAmountInMargin(user.getAmountInMargin() - totalCost);
            // update accountbalance in database
            jt.execute("UPDATE USERACCOUNTINFORMATION SET AMOUNT_IN_Margin = " + user.getAmountInMargin()
                    + " WHERE username = '" + user.getUsername() + "'");


            //create an investment_collection with collection id = users collection id number
            jt.execute("INSERT INTO Investment_Collection" +
                    " VALUES( " + user.getInvestment_collection_id() + " ," + investmentId + ")");

            // and investmentID that matches the investment_stock's id above.
            jt = new JdbcTemplate(dataSource);

            jt.execute("INSERT INTO Investment_Stock_short"
                    + " VALUES('" + investmentId + "' , '" + stock.getSymbol() + "' , "
                    + stock.getClose() + " ,'" + date + "' , " + stock.getNumberOfShares()
                    + ", " + user.getInvestment_collection_id() + ", 'NO')");
            jt.execute("INSERT INTO Investment_Stock"
                    + " VALUES('" + investmentId + "' , '" + stock.getSymbol() + "' , "
                    + stock.getClose() + " ,'" + date + "' , " + stock.getNumberOfShares()
                    + ", " + user.getInvestment_collection_id() + ", 'NO')");

            PurchasedStock newStock = new PurchasedStock();
            newStock.setInvestmentID(investmentId);
            newStock.setSymbol(stock.getSymbol());
            newStock.setPurchase_price(stock.getClose());
            newStock.setNumberOfShares(stock.getNumberOfShares());
            newStock.setPurchaseDate(stock.getDate());
            currentAccountMap.get(user.getUsername()).getCurrentStockInvestments().add(newStock);
            currentAccountMap.get(user.getUsername()).getCurrentStockInvestmentsShort().add(newStock);
            return true;
        }

    }
    // ========= END shortStock method =================================================================================

    /* =========== processShortedStocks method =========================================================================
        This method is used to fill the current users shorted stock portfolio. It uses the fillAllShortedStocks method
        to retrieve a list of the users purchased stocks from the database and saves them locally within an array list.
        input: user
        output: true or false depending on whether the user has at least one stock
     */
    @Override
    public boolean processShortedStocks(UserAccount user) {
        boolean hasStocks = false;
        List<PurchasedStock> stocks = findAllShortedStocks(user.getInvestment_collection_id());

        // logging information about the stocks and user
        log.info("purchasedStocks called, stock = : {}", stocks);
        log.info("purchasedStocks called, userId = : {}", user.getInvestment_collection_id());

        // filling the users shorted stock array list with stocks from the database
        for (PurchasedStock stock : stocks) {
            hasStocks = true;
            user.getCurrentStockInvestmentsShort().add(stock);
        }
        return hasStocks;
    }

    /* ========= findAllShortedStocks method ==================================================================================
        This method is used to retrieve all of the shorted stocks that a user has from the database.
        input: a users investmentCollectionId
        output: list of stocks
     */
    @Override
    public List<PurchasedStock> findAllShortedStocks(int investmentCollectionId) {
        jt = new JdbcTemplate(dataSource);

        //query string
        String sql = "select * from Investment_stock_short where" +
                " Investment_collection_id = " + investmentCollectionId
                + " AND returned = 'NO'";

        //query execution
        List<PurchasedStock> returnList = jt.query(
                sql,
                (rs, rowNum) ->
                        new PurchasedStock(
                                rs.getInt("INVESTMENT_ID"),
                                rs.getString("SYMBOL"),
                                rs.getDouble("PURCHASE_PRICE"),
                                rs.getString("PURCHASE_DATE"),
                                rs.getDouble("NUMBER_OF_SHARES")
                        )
        );

        // logging each stock
        for (PurchasedStock stock : returnList) {
            log.info("stock: {}", stock);
        }
        return returnList;
    }
    //========= END findAllStocks method ===============================================================================

    /* ========= ConfirmStockSale method ===============================================================================
        This method is used to sell a stock. First, it gets the cash flow of the sale by calling the getCurrentPrice
        method to determine the current price of the stock and multiplies that value by the number of shares being sold.
        This method then updates the new account balance both locally and in the database.
        input: the stock being sold, the user selling it
        output: void
     */
    @Override
    public void ConfirmStockSale(PurchasedStock stock, UserAccount account) {

        // calculating the cash flow
        double cashFlow = stock.getCurrentPrice() * stock.getNumberOfShares();

        //logging data about the sale
        log.info("STOCKS CURRENT PRICE : {}", stock.getCurrentPrice());
        log.info("STOCKS SHARE NUMBER : {}", stock.getNumberOfShares());
        log.info("CASH FLOW : {}", cashFlow);

        // updating the new account balance locally
        double newSavingsBalance = account.getAmountInSavings() + cashFlow;
        log.info("New Account Balance = {}", newSavingsBalance);
        account.setAmountInSavings(newSavingsBalance);

        //Marking the stock in the database as sold
        jt.execute("UPDATE Investment_stock SET SOLD = 'YES'"
                + " WHERE INVESTMENT_ID = " + stock.getInvestmentID());

        // updating the  account balance in database
        jt.execute("UPDATE USERACCOUNTINFORMATION SET AMOUNT_IN_SAVINGS = "
                + newSavingsBalance + " WHERE username = '" + account.getUsername() + "'");
    }
    //========= END ConfirmStockSale method ============================================================================

    /* ========= daysBorrowed method ===============================================================================
        This method is used to calculate the number of days between two days, more specifically, the date a stock
        was borrowed and the date of return.
        input: borrow date
        output: the days between the borrow date and the current date (long)
     */
    @Override
    public long daysBorrowed(String startDate) {

        LocalDate start = LocalDate.parse(startDate.trim());
        LocalDate now = LocalDate.now();

        long daysBetween = ChronoUnit.DAYS.between(start, now);

        return daysBetween;
    }
    //========= END daysBorrowed method ================================================================================

    /* ========= interestOnShort method ===============================================================================
        This method calculates the interest owed on a short sale.
        input: the total amount borrowed, the length of the borrow
        output: total amount of interest owed (double)
     */
    @Override
    public double interestOnShort(double totalPrice, long daysBorrowed) {
        double interestTotal = ((daysBorrowed / (double) 365) * 0.09) * totalPrice;
        return interestTotal;
    }
    //========= END interestOnShort method =============================================================================

    /* ========= processShortReturn method =============================================================================
       This method processes a short return. First, it ensures the user has enough in their savings to pay for the
       interest on the loan. Next, it makes sure the user has the borrowed stock in their stock portfolio as well
        them having the shorted stock within their shorted stock portfolio. Then, this method marks both the stock
        in the stock portfolio and the shorted stock portfolio as sold/returned. Then, this method updates the account
        balance and the amount available in margin based on the interest payed and the amount borrowed respectively.
       input: intested total, the stocks being returned, the user
       output: true or false based on whether the user can return the stock
    */
    @Override
    public boolean processShortReturn(double interestOwed, PurchasedStock stock, UserAccount user) {

        double startingSavingsAmount;
        double newSavingsAmount;
        int stockInvestmentId;
        int returnStockInvestmentId;
        double startingMarginAmount;
        double availableMarginIncrease = stock.getPurchase_price() * stock.getNumberOfShares();

        // YOU MUST PAY BACK ALL SHARES AT ONCE!! YOU MUST HAVE THE SHARES WITH THE EXACT QUANTITY IN YOUR PORTFOLIO
        //BEFORE FIRST!! Make sure the user has the stock in their portfolio and has enough in savings
        //to pay the interest!!!

        // query to check if the user can afford the interest
        String savingsQuery = "SELECT amount_in_savings FROM UserAccountInformation where username = '"
                + user.getUsername() + "'";
        startingSavingsAmount = jt.queryForObject(savingsQuery, Double.class);
        if (startingSavingsAmount < interestOwed) {
            return false;
        }

        // query to check if the user has the stock with the correct number of share in their portfolio
        String sql = "SELECT * FROM INVESTMENT_STOCK where INVESTMENT_COLLECTION_ID = "
                + user.getInvestment_collection_id() + " AND SYMBOL = '" + stock.getSymbol() + "' AND NUMBER_OF_SHARES = "
                + stock.getNumberOfShares() + " AND SOLD = 'NO'";

        List<PurchasedStock> returnList = jt.query(
                sql,
                (rs, rowNum) ->
                        new PurchasedStock(
                                rs.getInt(1),
                                rs.getString(2),
                                rs.getDouble(3),
                                rs.getString(4),
                                rs.getDouble(5)
                        )
        );

        if (returnList.isEmpty()) {
            return false;
        }

        stockInvestmentId = returnList.get(0).getInvestmentID();

        // query to get the users current margin balance
        String marginQuery = "SELECT amount_in_margin FROM UserAccountInformation where username = '"
                + user.getUsername() + "'";
        startingMarginAmount = jt.queryForObject(marginQuery, Double.class);


        // query to change the returned stock to sold
        String stockRemovalQuery = "UPDATE INVESTMENT_STOCK SET SOLD = 'YES' WHERE INVESTMENT_ID = " + stockInvestmentId;
        jt.execute(stockRemovalQuery);


        // query to retrieve the stock to be returned from the database
        String removeShortQuery = "SELECT * FROM INVESTMENT_STOCK_SHORT where  SYMBOL = '" + stock.getSymbol() +
                "' AND NUMBER_OF_SHARES = " + stock.getNumberOfShares() + " AND RETURNED = 'NO'";

        List<PurchasedStock> returnList2 = jt.query(
                removeShortQuery,
                (rs, rowNum) ->
                        new PurchasedStock(
                                rs.getInt(1),
                                rs.getString(2),
                                rs.getDouble(3),
                                rs.getString(4),
                                rs.getDouble(5)
                        )
        );

        if (returnList2.isEmpty()) {
            return false;
        }

        returnStockInvestmentId = returnList2.get(0).getInvestmentID();

        // query to mark the stock that will be returned to returned
        String stockShortRemovalQuery = "UPDATE INVESTMENT_STOCK_SHORT SET RETURNED = 'YES' WHERE INVESTMENT_ID = "
                + returnStockInvestmentId;
        jt.execute(stockShortRemovalQuery);

        //query to reduce the users savings balance by the amount owed in interest
        newSavingsAmount = startingSavingsAmount - interestOwed;
        String savingsUpdateQuery = "UPDATE USERACCOUNTINFORMATION SET AMOUNT_IN_SAVINGS = " + newSavingsAmount
                + " WHERE username = '" + user.getUsername() + "'";
        jt.execute(savingsUpdateQuery);


        //query to increase the amount available in margin by  purchase_price*number of shares.
        startingMarginAmount = startingMarginAmount + availableMarginIncrease;
        String marginUpdateQuery = "UPDATE USERACCOUNTINFORMATION SET AMOUNT_IN_MARGIN = " + startingMarginAmount
                + " WHERE username = '" + user.getUsername() + "'";
        jt.execute(marginUpdateQuery);
        return true;
    }
    //========= END processShortReturn method ==========================================================================

    /* ========= saveConfirmationToken method ==========================================================================
       This method saves a confirmation token in the database when a new user is registered which has a username
        associated with it.
       input: confirmation token
       output: void
    */
    public void saveConfirmationToken(ConfirmationToken token) {
        int nextTokenId = 0;
        //first find the highest confirmation token id.
        nextTokenId = getNextConfirmationTokenId();
        // set the current token id to the next highest
        token.setTokenId(nextTokenId);
        // insert the cofirmationToken into the database.
        jt = new JdbcTemplate(dataSource);
        jt.execute("INSERT INTO confirmation_token"
                + " VALUES(" + token.getTokenId() + " , '" + token.getConfirmationToken() + "' , '"
                + token.getCreatedDate() + "','" + token.getUsername() + "')");

    }
    // ======== END saveConfirmationToken ==============================================================================

    /* ========= getConfirmationToken method ==========================================================================
       This method is used to retrieve a confirmation token from the database
       input: token
       output: confirmation token
    */
    public ConfirmationToken getConfirmationToken(String token){
        jt = new JdbcTemplate(dataSource);

        // query string
        String sql = "select * from confirmation_token where" +
                " confirmation_token = '" + token
                + "'";

        // query execution
        List<ConfirmationToken> returnList = jt.query(
                sql,
                (rs, rowNum) ->
                        new ConfirmationToken(
                                rs.getInt("token_id"),
                                rs.getString("confirmation_token"),
                                rs.getString("created_date"),
                                rs.getString("username")
                        )

        );

        if(returnList.isEmpty()){
            return null;
        }
        if(returnList.size() > 1){
            return null;
        }
        return returnList.get(0);
    }

    //========= END getConfirmationToken method ========================================================================

    /* ========= enableUser method ==========================================================================
       This method is used to mark a users account as enabled
       input: username
       output: void
    */
    public void enableUser(String username){
        jt.execute("UPDATE moneymanagerusers SET enabled = 'Y'"
                + " WHERE username = '" + username  + "'");
        log.info("{} enabled", username);
    }

}
