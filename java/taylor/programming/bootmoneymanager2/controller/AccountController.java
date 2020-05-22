package taylor.programming.bootmoneymanager2.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import taylor.programming.bootmoneymanager2.model.PurchasedStock;
import taylor.programming.bootmoneymanager2.model.Stock;
import taylor.programming.bootmoneymanager2.model.UserAccount;
import taylor.programming.bootmoneymanager2.service.UserAccountService;
import taylor.programming.bootmoneymanager2.util.AttributeNames;
import taylor.programming.bootmoneymanager2.util.Mappings;
import taylor.programming.bootmoneymanager2.verification.ConfirmationToken;
import taylor.programming.bootmoneymanager2.verification.EmailSenderService;

import java.text.NumberFormat;

@Slf4j
@Controller
public class AccountController {

    public final UserAccountService service;

    @Autowired
    public AccountController(UserAccountService service){
        this.service = service;
    }

    @Autowired
    private EmailSenderService emailSenderService;

    /* ================ toDashBoard method ======================================================
    When a user goes to the dashboard, most of their information within the database is retrieved
    and the model is updated with that information. Model attributes are created corresponding
    to the users data, then sent to the view.
    input:  sign in credentials
    output: dashboard view customized with respect to the user
     */
    @GetMapping(Mappings.DASHBOARD)
    public String toDashBoard(Model model){

        boolean stockPorfolioHasContent = true;
        boolean shortPortfolioHasContent = true;

        //getting the current users username and filling the rest of the account information out based on the username
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentPrincipalName = authentication.getName();
        service.fillInUserInfo(currentPrincipalName);

        //formatting account balances in currency format
        NumberFormat moneyFormat = NumberFormat.getCurrencyInstance();
        String moneyValue = moneyFormat.format(
                service.getCurrentAccount(currentPrincipalName).getAmountInSavings());
        String marginAvailable = moneyFormat.format(
                service.getCurrentAccount(currentPrincipalName).getAmountInMargin());

        //updating the stock portfolio
        service.getCurrentAccount(currentPrincipalName).clearCurrentStocks();
        service.processPurchasedStocks(service.getCurrentAccount(currentPrincipalName));

        //updating the short portfolio
        service.getCurrentAccount(currentPrincipalName).clearShortedStocks();
        service.processShortedStocks(service.getCurrentAccount(currentPrincipalName));

        //checking if the portfolios are empty, if so, setting boolean values to false which will
        //be used by javascript to dynamically change the view
        if(service.getCurrentAccount(currentPrincipalName).getCurrentStockInvestments().isEmpty()){
            stockPorfolioHasContent = false;
        }


        if(service.getCurrentAccount(currentPrincipalName).getCurrentStockInvestmentsShort().isEmpty()){
            shortPortfolioHasContent = false;
        }

        // adding model attributes which correspond to the users account information
        model.addAttribute(AttributeNames.USER_ACCOUNT_NAME,
                service.getCurrentAccount(currentPrincipalName).getUsername());
        model.addAttribute(AttributeNames.FIRST_NAME,
                service.getCurrentAccount(currentPrincipalName).getFirstName());
         model.addAttribute(AttributeNames.ACCOUNT_BALANCE,
                 moneyValue);
         model.addAttribute(AttributeNames.MARGIN_BALANCE,
                 marginAvailable);
        model.addAttribute(AttributeNames.STOCK_ARRAY,
                service.getCurrentAccount(currentPrincipalName).getCurrentStockInvestments());

        model.addAttribute(AttributeNames.SHORT_ARRAY,
                service.getCurrentAccount(currentPrincipalName).getCurrentStockInvestmentsShort());

        model.addAttribute(AttributeNames.STOCK_PORTFOLIO_HAS_CONTENT,
                stockPorfolioHasContent);

        model.addAttribute(AttributeNames.SHORT_PORTFOLIO_HAS_CONTENT,
                shortPortfolioHasContent);

        log.info("Get Dashboard called, model = {}" , model);
        return Mappings.DASHBOARD;
    }
    //================ END toDashBoard method ======================================================


    /* ===================== postLogin method ===========================================
    This method is called after a successful login. A UserAccount object is created with
    username corresponding to the login credentials which is then added to the services
    hashmap.
    input: correct user credentials
    output: dashboard view
     */
    @GetMapping (Mappings.PERFORM_LOGIN)
    public String postLogin(Model model){

        // getting the current users username
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentPrincipalName = authentication.getName();

        // creating UserAccount object which corresponds to the users information
        UserAccount user = new UserAccount();
        user.setUsername(currentPrincipalName);

        // adding the UserAccount object to the service hashmap
        service.setCurrentAccount(currentPrincipalName, user);

        //logging some data
        log.info("model: {}" , model);
        log.info("PostMapping login called, name : {}", currentPrincipalName);
        return "redirect:/" + Mappings.DASHBOARD;
    }
    //================ END postLogin method =======================================================

    /* =============== toLogOut method ============================================================
    This method is called when a user goes to the logout tab. It removes the UserAccount object
    within the service hashmap which corresponds to  the current users information.
     */
    @GetMapping(Mappings.PRE_LOGOUT)
    public String toLogOut(){
        log.info("toLogOut called");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentPrincipalName = authentication.getName();
        service.removeCurrentAccount(currentPrincipalName);
        log.info("Removed from service: {}", currentPrincipalName);
        return "redirect:/" + Mappings.LOGOUT;
    }
    // ================ END toLogOut method =======================================================



    /* ================ doRegister method =======================================================
    This method is called when a user POSTS information from the register page. It will check if
    the submitted email is valid (doesn't already exist or is in a valid format) as well as check
    the rest of the users input for validity. If there is any invalid information, a view will be
    returned based on which information was invalid. If all the information is valid, a
    confirmation email will be send and the corresponding view will be displayed
    input: username, password, first name, last name, email
    output: invalid email page OR invalid username page OR invalid info page OR
    (verification email page and verification email)
     */
    @PostMapping(Mappings.REGISTER)
    public String doRegister(@ModelAttribute(AttributeNames.REGISTER_ACCOUNT) UserAccount account ){


        //verify the users email doesn't exist in the database and is valid
        boolean isValidEmail = service.checkValidEmail(account.getEmail());

        if(!isValidEmail){
            //email Either doesn't exist or is invalid format
            return Mappings.ACCOUNT_CREATION + Mappings.FAILED_ACCOUNT_CREATION_INVALID_EMAIL;
        }else {


            // validating user info
            int saveUserReturnValue = service.saveUser(account); //2 == info invalid, 1 == username exists, 0 == all good

            if (saveUserReturnValue == 1) {
                return Mappings.ACCOUNT_CREATION + Mappings.FAILED_ACCOUNT_CREATION_ALREADY_EXISTS;
            }

            if (saveUserReturnValue == 2) {
                return Mappings.ACCOUNT_CREATION + Mappings.FAILED_ACCOUNT_CREATION_INVALID_INFO;
            }


            //creating and saving a confirmation token
            ConfirmationToken confirmationToken = new ConfirmationToken(account);
            service.saveConfirmationToken(confirmationToken);

            // sending validation email
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setTo(account.getEmail());
            mailMessage.setSubject("Complete Bigshort Registration");
            mailMessage.setFrom("tcstoltzfus@gmail.com");
            mailMessage.setText("To confirm your account, click the following link and sign in with your account information: "
                    + "https://bigshort.org/confirm-account?token=" + confirmationToken.getConfirmationToken());

            emailSenderService.sendMail(mailMessage);


            return Mappings.ACCOUNT_CREATION + Mappings.VERIFICATION_EMAIL_SENT;
        }
    }
    // ================ END doRegister method =======================================================

    /* ================ toConfirmAccount method =======================================================
    This method passes the string parameter it receives to the enableUser method of the service class.
    Each confirmation token in the database has a corresponding username, this method enables the
    username that is associated with the parameter confirmation token and returns the view to the login
    input: confirmation token
    output: login view
     */

    @GetMapping(Mappings.CONFIRM_ACCOUNT)
    public String toConfirmAccount(@RequestParam("token") String confirmationToken){
        ConfirmationToken token = service.getConfirmationToken(confirmationToken);

        if(token != null){
            //enable user based on token
            service.enableUser(token.getUsername());
        }

        return "redirect:/" + Mappings.LOGIN;
        //todo create multiple redirect mappings
    }
    // ================ END toConfirmAccount method =======================================================


    /* ================ toPurchasePage method =======================================================
    This method retrieves user account balances and stock information and sends the corresponding
    data to the purchase page view.
    input: stock information
    output: stock information, account balance, purchase_page view
     */
    @PostMapping(Mappings.PURCHASE_PAGE)
    public String toPurchasePage(@ModelAttribute(AttributeNames.STOCK) Stock stock, Model model){

        // getting the current users username
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentPrincipalName = authentication.getName();

        // formatting account balances to currency format
        NumberFormat moneyFormat = NumberFormat.getCurrencyInstance();
        String moneyValue = moneyFormat.format(
                service.getCurrentAccount(currentPrincipalName).getAmountInSavings());
        String marginValue = moneyFormat.format(
                service.getCurrentAccount(currentPrincipalName).getAmountInMargin());

        // adding balance and stock information to model using model attributes
        model.addAttribute(AttributeNames.STOCK, stock);
        model.addAttribute(AttributeNames.STOCK_SYMBOL, stock.getSymbol());
        model.addAttribute(AttributeNames.STOCK_OPEN, stock.getOpen());
        model.addAttribute(AttributeNames.STOCK_HIGH, stock.getHigh());
        model.addAttribute(AttributeNames.STOCK_LOW, stock.getLow());
        model.addAttribute(AttributeNames.STOCK_VOLUME, stock.getVolume());
        model.addAttribute(AttributeNames.STOCK_CLOSE, stock.getClose());
        model.addAttribute(AttributeNames.STOCK_DATE, stock.getDate());
        model.addAttribute(AttributeNames.ACCOUNT_BALANCE, moneyValue);
        model.addAttribute(AttributeNames.MARGIN_BALANCE, marginValue);
        model.addAttribute(AttributeNames.SHARE_AMOUNT,
                service.getCurrentAccount(currentPrincipalName).getCurrentBrowsingStock().numberOfShares);

        // outputting some log information
        log.info("PRICE: {}", stock.close);
        log.info("LOW: {}", stock.low);
        log.info("HIGH: {}" , stock.high);
        log.info("Symbol: {}" , stock.symbol);

        return Mappings.PURCHASE + Mappings.PURCHASE_PAGE;
    }
    // ================ END toPurchasePage method =======================================================

    /* ================ sellStock method ================================================================
    This method receives a stock as a parameter and uses other methods to determine that stocks current
    price and the total gains and loses from the stock if it would be sold. Then, this method adds
    all that data to the model in the form of model attributes.
    input: stock to be sold
    output: view with corresponding data
     */
    @PostMapping(Mappings.SELL_STOCK)
    public String sellStock(@ModelAttribute(AttributeNames.SELL_STOCK)PurchasedStock stock, Model model){

        // determining the current price of the stock as well as potential gains and loses
        stock.setCurrentPriceData(service.getAPIprefix(), service.getAPIkey());
        stock.setTotalGainsAndLoses();

        // converting gains and loses to currency format
        NumberFormat moneyFormat = NumberFormat.getCurrencyInstance();
        String moneyValue = moneyFormat.format(stock.getTotalGainsAndLoses());

        //adding the data to the model in the form of model attributes
        model.addAttribute(AttributeNames.STOCK_ID, stock.getInvestmentID());
        model.addAttribute(AttributeNames.STOCK_SYMBOL, stock.getSymbol());
        model.addAttribute(AttributeNames.STOCK_PRICE, stock.getPurchase_price());
        model.addAttribute(AttributeNames.SHARE_AMOUNT, stock.getNumberOfShares());
        model.addAttribute(AttributeNames.CURRENT_PRICE, stock.getCurrentPrice());
        model.addAttribute(AttributeNames.TOTAL_GAINS_AND_LOSES, stock.getTotalGainsAndLoses());
        model.addAttribute(AttributeNames.GAINS_STRING, moneyValue);

        //logging some information about the stock
        log.info("MONEY VALUE : {}", moneyValue);
        log.info("INVESTMENT ID: {}", stock.getInvestmentID());

        return Mappings.SELLING + Mappings.SELL_STOCK;
    }
    // ================ END sellStock method ===========================================================

    /* ================ confirmSale method =============================================================
    This method receives a stock as a parameter and uses other methods to process the sale of the stock.
    Then, this method returns the view to the dashboard
    input: stock to be sold
    output: dashboard view
     */
    @PostMapping(Mappings.CONFIRM_SALE)
    public String confirmSale(@ModelAttribute(AttributeNames.SELL_STOCK)PurchasedStock stock){

        //determining the current user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentPrincipalName = authentication.getName();

        //using the service to process the stock sale
        service.ConfirmStockSale(stock, service.getCurrentAccount(currentPrincipalName));

        return "redirect:/" + Mappings.DASHBOARD;
    }
    // ================ END confirmSale method ===============================================================

    /* ================ returnStock method =============================================================
    This method receives a stock as parameter, which is going to be returned from the short portfolio.
    The method calculates the interest owed on the stock based on the price and length of the borrow.
    input: stock
    output: dashboard view
     */
    @PostMapping(Mappings.RETURN_STOCK)
    public String returnStock(@ModelAttribute(AttributeNames.SELL_STOCK) PurchasedStock stock, Model model){

        // used for calculating interest
        long daysBorrowed = service.daysBorrowed(stock.getPurchaseDate());
        double totalPrice = stock.getPurchase_price() * stock.getNumberOfShares();
        double interestOwed = service.interestOnShort(totalPrice, daysBorrowed);
        NumberFormat moneyFormat = NumberFormat.getCurrencyInstance();
        String moneyValue = moneyFormat.format(interestOwed);

        //adds information about the stock loan to the model in the form of model attributes
        model.addAttribute(AttributeNames.STOCK_SYMBOL, stock.getSymbol());
        model.addAttribute(AttributeNames.STOCK_PRICE, stock.getPurchase_price());
        model.addAttribute(AttributeNames.SHARE_AMOUNT, stock.getNumberOfShares());

        model.addAttribute(AttributeNames.DAYS_BORROWED,
                daysBorrowed);
        model.addAttribute(AttributeNames.INTEREST_OWED_STRING,
               moneyValue);
        model.addAttribute(AttributeNames.INTEREST_OWED,
                interestOwed);

        return Mappings.SELLING + Mappings.RETURN_STOCK;
    }
    // ================ END returnStock method ===============================================================


     /* ================ confirmReturn method ==================================================================
    This method receives a stock as a parameter and uses other methods to process the return of the stock.
    Afterward, the method changes the view to the dashboard
    input: stock to be returned
    output: dashboard view
     */
    @PostMapping(Mappings.CONFIRM_RETURN)
    public String confirmReturn(@RequestParam double interestOwed,
                                @ModelAttribute(AttributeNames.SELL_STOCK) PurchasedStock stock){

        //determining the current users username
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentPrincipalName = authentication.getName();

        //attempting to process the return if the stock and logging the result
        if(service.processShortReturn( interestOwed, stock, service.getCurrentAccount(currentPrincipalName))){
            log.info("RETURN SUCCESS");
        }else{
            log.info("Return Failed");
        }

        return "redirect:/" + Mappings.DASHBOARD;
    }
    // ================ END confirmReturn method ===============================================================

    /* ================ processPurchase method ==================================================================
   This method receives an action (pay with cash or short the stock), a share amount, and a stock as parameters.
   Then, this method process the stock based on the information that was provided as parameters.
   input: action, share amount, stock
   output: dashboard view, stock processed
    */
    //TODO ENDED HERE WITH COMMENTS.
    @PostMapping(Mappings.PURCHASE_PROCESSING)
    public String processPurchase(@RequestParam String action, @RequestParam(value = "shareAmount", required = false)
            Integer shareAmount, @ModelAttribute(AttributeNames.STOCK) Stock number){

        //determining the current users username
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentPrincipalName = authentication.getName();


        if(shareAmount == null){
            // TODO NEED TO REDIRECT TO SOME FORM OF ERROR IF THEY ENTER 0 SHARES, FORNOW DASHBOARD REDIRECT
            return "redirect:/" + Mappings.DASHBOARD;
        }else{
            number.setNumberOfShares(shareAmount);
        }


        //processing the stock if pay with cash is selected
        if(action.equals("Cash")){
            log.info("Cash");

            if(service.processStock(number,
                    service.getCurrentAccount(currentPrincipalName))){
                return "redirect:/" + Mappings.DASHBOARD;
            }else{//TODO NEED TO CREATE A SCREEN FOR WHEN PURCHASE FAILS
                return "redirect:/" + Mappings.DASHBOARD;
            }

        }else if(action.equals("Margin")){
            log.info("Margin");


            //processing the stock if short is selected
        }else if(action.equals("Short")){
            if(service.shortStock(number,
                service.getCurrentAccount(currentPrincipalName))){
                return "redirect:/" + Mappings.DASHBOARD;
            }else{//TODO NEED TO CREATE A SCREEN FOR WHEN SHORT FAILS
                return "redirect:/" + Mappings.DASHBOARD;
            }

        }

        // logging some information
        log.info("SHARE AMOUNT : {}", number.numberOfShares);
        log.info("SYMBOL : {}", number.symbol);
        log.info("Close: {}", number.close);
        log.info("Date: {}", number.getDate());

        return Mappings.DASHBOARD;
    }
    //================END processPurchase method ================================================================


    /* ================ toDisplay method ==================================================================
   This method returns the view for displaying a stocks information and a graph. API keys are retrieved from
   the service and sent to the view within a model attribute.
   input: stock
   output: research display view
    */
    @PostMapping(Mappings.RESEARCHDISPLAY)
    public String toDisplay(@ModelAttribute(AttributeNames.STOCK) Stock stock, Model model){

        // logging some information
        log.info("Symbol = {}" , stock.symbol);
        log.info("Symbol = {}" , model.getAttribute(AttributeNames.STOCK));

        // adding model attributes
        model.addAttribute(AttributeNames.STOCK, stock);
        model.addAttribute(AttributeNames.STOCK_SYMBOL, stock.symbol);
        model.addAttribute(AttributeNames.STOCK_OPEN, stock.getOpen());
        model.addAttribute("APIprefix",
                service.getAPIprefix());
        model.addAttribute("APIkey",
                service.getAPIkey());

        // determining the current users username
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentPrincipalName = authentication.getName();
        service.getCurrentAccount(currentPrincipalName).setCurrentBrowsingStock(stock);

        return Mappings.PURCHASE + Mappings.RESEARCHDISPLAY;
    }
    //================END toDisplay method ================================================================


    // ============= THE REST OF METHODS RETURN VIEWS NEAR EXCLUSIVELY ====================================

    @GetMapping(Mappings.ABOUT)
    public String toAbout(){

        return Mappings.ABOUT;
    }

    @GetMapping(Mappings.ABOUTSI)
    public String toAboutSignedIn(){

        return Mappings.ABOUTSI;
    }

    @GetMapping(Mappings.RESEARCH)
    public String toResearch(){

        return Mappings.PURCHASE + Mappings.RESEARCH;
    }

    @GetMapping(Mappings.VERIFICATION_EMAIL_SENT)
    public String toVerificationEmailSent(){
        return Mappings.VERIFICATION_EMAIL_SENT;
    }

    @GetMapping(Mappings.REGISTER)
    public String getRegister(Model model){
        UserAccount newAccount = new UserAccount();
        model.addAttribute(AttributeNames.REGISTER_ACCOUNT, newAccount);
        log.info("getRegister called");
        return Mappings.ACCOUNT_CREATION + Mappings.REGISTER;

    }

    @GetMapping(Mappings.ACCOUNT_INFO)
    public String toAccount(Model model){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentPrincipalName = authentication.getName();
        model.addAttribute(AttributeNames.USER_ACCOUNT_NAME, service.getCurrentAccount(currentPrincipalName).getUsername());

        return Mappings.ACCOUNT_INFO;
    }

    @GetMapping(Mappings.LOGIN)
    public String toLogin(){
        return Mappings.LOGIN;
    }

    @GetMapping("login_error1")
    public String toLoginPost(Model model){
        model.addAttribute(AttributeNames.LOGIN_ERROR,
                true);
        return Mappings.LOGIN;
    }

}
