# purchase-exchange-service
A service to store purchase transactions in USD and retrieve them converted to Treasury-supported currencies based on historical exchange rates.

## Requirement
### Requirement #1: Store a Purchase Transaction
Your application must be able to accept and store (i.e., persist) a purchase transaction with a description, transaction
date, and a purchase amount in United States dollars. When the transaction is stored, it will be assigned a unique
identifier.

**Field requirements :**

● Description: must not exceed 50 characters

● Transaction date: must be a valid date format

● Purchase amount: must be a valid positive amount rounded to the nearest cent

● Unique identifier: must uniquely identify the purchase

### Requirement #2: Retrieve a Purchase Transaction in a Specified Country’s Currency
Based upon purchase transactions previously submitted and stored, your application must provide a way to retrieve the
stored purchase transactions converted to currencies supported by the Treasury Reporting Rates of Exchange API based
upon the exchange rate active for the date of the purchase.  
https://fiscaldata.treasury.gov/datasets/treasury-reporting-rates-exchange/treasury-reporting-rates-of-exchange
The retrieved purchase should include the identifier, the description, the transaction date, the original US dollar purchase
amount, the exchange rate used, and the converted amount based upon the specified currency’s exchange rate for the
date of the purchase.

**Currency conversion requirements :**

● When converting between currencies, you do not need an exact date match, but must use a currency conversion
rate less than or equal to the purchase date from within the last 6 months.

● If no currency conversion rate is available within 6 months equal to or before the purchase date, an error should
be returned stating the purchase cannot be converted to the target currency.  

● The converted purchase amount to the target currency should be rounded to two decimal places (i.e., cent).

## Build the app
mvn clean install

## Run the app
docker compose up --build

## API

### Create Purchase
POST /purchases
{
"description": "Book",
"transactionDate": "2024-01-01",
"amount": 10.50
}

### Convert Currency
GET /purchases/{id}/convert?currency=Euro&country=Euro Zone

### Swagger UI
http://localhost:8080/swagger-ui/index.html
