package com.bank.product.domain.catalog.model;

/**
 * Product types supported by the catalog system.
 * Includes both account types and payment processing services.
 */
public enum ProductType {
    // Account Products
    CHECKING_ACCOUNT,
    SAVINGS_ACCOUNT,
    MONEY_MARKET_ACCOUNT,
    CERTIFICATE_OF_DEPOSIT,
    CREDIT_CARD,
    PERSONAL_LOAN,
    MORTGAGE,
    BUSINESS_ACCOUNT,
    INVESTMENT_ACCOUNT,
    CASH_MANAGEMENT,
    TREASURY_SERVICE,

    // Payment Processing Products
    ACH_TRANSFER,
    WIRE_TRANSFER,
    REAL_TIME_PAYMENT,
    P2P_PAYMENT,
    BILL_PAYMENT,
    CARD_PAYMENT,

    OTHER
}