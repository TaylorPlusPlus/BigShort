package taylor.programming.bootmoneymanager2.service;

import taylor.programming.bootmoneymanager2.model.PurchasedStock;
import taylor.programming.bootmoneymanager2.model.Stock;
import taylor.programming.bootmoneymanager2.model.UserAccount;
import taylor.programming.bootmoneymanager2.verification.ConfirmationToken;

import java.util.List;

public interface UserAccountService {

    UserAccount getCurrentAccount(String username);
    void setCurrentAccount(String username, UserAccount account);
    void removeCurrentAccount(String username);
    String showAll();
    int saveUser(UserAccount account);
    boolean checkValidUsername(String username);
    boolean checkValidPassword(String password);
    boolean checkValidFirstOrLastName(String firstName);
    boolean checkValidEmail(String email);
    int getNextInvestmentCollectionId();
    int getNextBadgeCollectionId();
    void fillInUserInfo(String username);
    int getNextInvestmentId();
    boolean processPurchasedStocks(UserAccount user);
    List<PurchasedStock> findAllStocks(int investmentCollectionId);
    void ConfirmStockSale(PurchasedStock stock, UserAccount account);
    boolean shortStock(Stock stock, UserAccount user);
    boolean processShortedStocks(UserAccount user);
    List<PurchasedStock> findAllShortedStocks(int investmentCollectionId);
    boolean processStock(Stock stock, UserAccount user);
   // boolean ReturnShortedStock(PurchasedStock stock, UserAccount account);
    long daysBorrowed(String startDate);
    double interestOnShort(double totalPrice, long daysBorrowed);
    boolean processShortReturn(double interestOwed, PurchasedStock stock, UserAccount user);
    void saveConfirmationToken(ConfirmationToken token);
    ConfirmationToken getConfirmationToken(String token);
    void enableUser(String username);
    String getAPIkey();
    String getAPIprefix();
}
