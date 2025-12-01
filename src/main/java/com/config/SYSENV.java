/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.config;

import com.queue.QueueProducer;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import philae.ach.UsRole;

/**
 *
 * @author melleji.mollel
 */
@Component
public class SYSENV {

    @Autowired
    @Qualifier("jdbcCbsLive")
    JdbcTemplate jdbcRUBIKONTemplate;

    @Autowired
    @Qualifier("amgwdb")
    JdbcTemplate jdbcTemplate;

    @Value("${sender.swiftcode}")
    public String SENDER_BIC;
    @Value("${bot.swiftcode}")
    public String BOT_SWIFT_CODE;
    @Value("${mt103.usd.correspondentBank}")
    public String USD_CORRESPONDEND_BANK;
    @Value("${mt103.euro.correspondentBank}")
    public String EURO_CORRESPONDEND_BANK;
    @Value("${mt103.ZAR.correspondentBank}")
    public String ZAR_CORRESPONDEND_BANK;
    @Value("${mt103.GBP.correspondentBank}")
    public String GBP_CORRESPONDEND_BANK;
    @Value("${vat.ledger: -1}")
    public String TRANSFER_AWAITING_VAT_LEDGER;
    @Value("${spring.ta.gl.account.tiss}")
    public String TRANSFER_AWAITING_TISS_LEDGER;
    @Value("${spring.ta.gl.account.bot.tiss}")
    public String TRANSFER_MIRROR_TISS_BOT_LEDGER;
    @Value("${spring.ta.gl.account.tt}")
    public String TRANSFER_AWAITING_TT_LEDGER;
    @Value("${spring.ta.gl.account.eft}")
    public String TRANSFER_AWAITING_EFT_LEDGER;
    @Value("${spring.ta.gl.account.bot.eft}")
    public String TRANSFER_MIRROR_EFT_BOT_LEDGER;
    @Value("${spring.ta.gl.account.hpension.eft:-1}")
    public String TRANSFER_MIRROR_EFT_HPENSION_LEDGER;
    @Value("${mt103.TT.SCBCorresponding.account}")
    public String TRANSFER_MIRROR_TT_SCB_LEDGER;
    @Value("${mt103.TT.BHFCorresponding.account}")
    public String TRANSFER_MIRROR_TT_BHF_LEDGER;
    @Value("${kprinter.rtgs.file.url}")
    public String KPRINTER_URL;
    @Value("${spring.eft.bot.tach.keypass}")
    public String PRIVATE_EFT_TACH_KEYPASS;
    @Value("${spring.eft.bot.tach.keyalias}")
    public String PRIVATE_EFT_TACH_KEY_ALIAS;
    @Value("${spring.eft.bot.tach.keypath}")
    public String PRIVATE_EFT_TACH_KEY_FILE_PATH;
    @Value("${spring.eft.bot.tach.publicKeypass}")
    public String PUBLIC_EFT_TACH_KEYPASS;
    @Value("${spring.eft.bot.tach.publicKeyalias}")
    public String PUBLIC_EFT_TACH_KEY_ALIAS;
    @Value("${spring.eft.bot.tach.pubkeypath}")
    public String PUBLIC_EFT_TACH_KEY_FILE_PATH;
    @Value("${corebanking.api.payments}")
    public String CORE_BANKING_POSTING_URL;
    @Value("${corebanking.api.test.payments}")
    public String CORE_BANKING_TEST_POSTING_URL;
    @Value("${corebanking.api.payments.rtgs}")
    public String CORE_BANKING_POSTING_URL_RTGS;
    @Value("${corebanking.api.tach.payments.rtgs: -1}")
    public String CORE_BANKING_POSTING_URL_RTGS_TACH;
    @Value("${eft.broker.url}")
    public String EFT_BROKER_URL;
    @Value("${spring.eft.transaction.session.time}")
    public String EFT_TRANSACTION_SESSION_TIME;
    @Value("${spring.sms.for.txns.onWorkflow}")
    public String SMS_NOTIFICATION_FOR_TXNS_ON_WORKFLOW;
    @Value("${spring.SMSC.url}")
    public String SMSC_URL;
    @Value("${spring.insurance.commission.account}")
    public String INSURANCE_COMMISSION_GL_ACCOUNT;
    @Value("${spring.eft.hq.ledger}")
    public String EFT_HQ_TRANSFER_AWAITING;
    @Value("${spring.tanescosaccoss.account}")
    public String TANESCO_SACCOSS_COLLECTION;
    @Value("${AIRTEL_DGS_COLLECTION_GL:-1}")
    public String AIRTEL_DGS_COLLECTION_GL;
    @Value("${TIGO_DGS_COLLECTION_GL:-1}")
    public String TIGO_DGS_COLLECTION_GL;
    @Value("${HALOTEL_DGS_COLLECTION_GL:-1}")
    public String HALOTEL_DGS_COLLECTION_GL;
    @Value("${MKOBA_DGS_COLLECTION_GL:-1}")
    public String MKOBA_DGS_COLLECTION_GL;
    @Value("${spring.EFT.CHARGE:-1}")
    public BigDecimal EFT_TXN_CHARGE;
    @Value("${DISABLE_CROSS_CURRENCY:Y}")
    public String DISABLE_CROSS_CURRENCY;
    @Value("${spring.TT.CHARGE:-1}")
    public BigDecimal TT_TXN_CHARGE;
    @Value("${spring.TISS.CHARGE:-1}")
    public BigDecimal TISS_TXN_CHARGE;
    @Value("${wallet.MPESAB2C.LEDGER:-1}")
    public String MPESAB2C_LEDGER;
    @Value("${wallet.TIGOPESAB2C.LEDGER:-1}")
    public String TIGOPESAB2C_LEDGER;
    @Value("${wallet.AIRTELB2C.LEDGER:-1}")
    public String AIRTELB2C_LEDGER;
    @Value("${wallet.HALOPESAB2C.LEDGER:-1}")
    public String HALOPESAB2C_LEDGER;
    @Value("${wallet.EZYPESAB2C.LEDGER:-1}")
    public String EZYPESAB2C_LEDGER;
    @Value("${gw.payment.url:-1}")
    public String GW_PAYMENT_URL;
    @Value("${spring.profiles.active:-1}")
    public String ACTIVE_PROFILE;
    @Value("${spring.TRA.EXERCISE.DUTY:-1}")
    public String EXERCISE_DUTY_LEDGER;
    @Value("${spring.TRA.VAT:-1}")
    public String VAT_LEDGER;
    @Value("${spring.EFT.INCOME.LEDGER:-1}")
    public String EFT_INCOME_LEDGER;
    @Value("${spring.TIS.INCOME.LEDGER:-1}")
    public String TIS_INCOME_LEDGER;
    @Value("${wallet.AZAM.COLLECTION.ACCOUNT:-1}")
    public String AZAM_COLLECTION_ACCOUNT;
    @Value("${wallet.DSTV.COLLECTION.ACCOUNT:-1}")
    public String DSTV_COLLECTION_ACCOUNT;
    @Value("${wallet.LUKU.COLLECTION.ACCOUNT:-1}")
    public String LUKU_COLLECTION_ACCOUNT;
    @Value("${wallet.LUKU.SUSPENSE.ACCOUNT:-1}")
    public String LUKU_SUSPENSE_ACCOUNT;
    //wallet.STARTIMES.COLLECTION.ACCOUNT
    @Value("${wallet.STARTIMES.COLLECTION.ACCOUNT:-1}")
    public String STARTIMES_COLLECTION_ACCOUNT;
    @Value("${mt103.TT.SCBSOUTHAFRICA.account:-1}")
    public String TRANSFER_MIRROR_TT_SBZAZA_LEDGER;
    @Value("${ibank.registration.url:-1}")
    public String IBANK_REGISTRATION_URL;
    @Value("${BRINJAL_OUTSTANDING_LOANS_URL:http://172.20.1.26:8090/esb/loan/reports/outstandingLoans}")
    public String BRINJAL_OUTSTANDING_LOANS_URL;
    @Value("${ibank.password.reset.url:-1}")
    public String IBANK_PASSWORD_RESET_URL;
    @Value("${PENSION_DEBIT_GL_ACCOUNT:1-070-00-2064-2064002}")
    public String PENSION_DEBIT_GL_ACCOUNT;
    @Value("${popote.app.registration.url:-1}")
    public String POPOTE_APP_REGISTRATION_URL;
    @Value("${spring.outgoing.waived.accounts: -1}")
    public String WAIVED_ACCOUNTS_LISTS;
    @Value("${spring.domain.controller.ip:-1}")
    public String DOMAIN_CONTROLLER_LDAP_AUTHENTICATION;
    @Value("${spring.domain.controller.port: -1}")
    public String DOMAIN_CONTROLLER_PORT;
    @Value("${spring.domain.controller.name: -1}")
    public String DOMAIN_CONTROLLER_NAME;
    @Value("${cilantro.bcx.pgp.public.key: -1}")
    public String BCX_PGP_PUBLIC_KEY;
    @Value("${cilantro.bcx.url: -1}")
    public String BCX_HTTP_URL;
    @Value("${cilantro.bcx.pgp.private.key: -1}")
    public String BCX_PGP_PRIVATE_KEY;
    @Value("${cilantro.tcbbank.visacards.bin: -1}")
    public String TCB_BANK_VISA_CARDS_BIN;
    @Value("${individual.stp.amount.without.ibd.approval: 2000000}")
    public String IND_AMOUNT_WITHOUT_HQ_APPROVAL;
    @Value("${corporate.stp.amount.without.ibd.approval: 50000000}")
    public String CORPORATE_AMOUNT_WITHOUT_HQ_APPROVAL;
    @Value("${kipayment.gepg.validate.controlNo.url: -1}")
    public String GePG_KIPAYMENT_VALIDATE_CONTROL_NO_URL;
    @Value("${cilantro.TRA.spcodes.high.values.mappings: -1}")
    public String TRA_ACCOUNT_HIGH_VALUES_SPCODE_MAPPINGS;
    @Value("${cilantro.card.registration.url: -1}")
    public String CARD_REGISTRATION_URL;
    @Value("${bot.tiss.vpn.url: -1}")
    public String BOT_TISS_VPN_URL;
    @Value("${bot.is.tiss.vpn.allowed: false}")
    public boolean IS_TISS_VPN_ALLOWED;

    @Value("${ALLOWED_STP_ACCOUNT_THROUGH_TISS_VPN: false}")
    public boolean ALLOWED_STP_ACCOUNT_THROUGH_TISS_VPN;

    @Value("${tcb.logo.path:-1}")
    public String TCB_LOGO_PATH;

    @Value("${brela.logo.path:-1}")
    public String BRELA_LOGO_PATH;

    @Value("${IS_GEPG_ALLOWED_THROUGH_TISS_VPN: false}")
    public boolean IS_GEPG_ALLOWED_THROUGH_TISS_VPN;

    @Value("${psssf.url: -1}")
    public String PSSSF_URL;
    //MoF CASH MANAGEMENT
    @Value("${cilantro.mof.cash.management.keypass: -1}")
    public String MOF_CASH_MANAGEMENT_KEYPASS;
    @Value("${cilantro.mof.cash.management.pfx.keypath:-1}")
    public String MOF_CASH_MANAGEMENT_PFX_KEY_FILE_PATH;
    @Value("${cilantro.bot.cash.management.cer.keypath:-1}")
    public String BOT_CASH_MANAGEMENT_CERT_KEY_FILE_PATH;
    @Value("${cilantro.mof.cash.management.pfx.privatekeypath:-1}")
    public String MOF_CASH_MANAGEMENT_PFX_PRIVATE_KEY_FILE_PATH;
    //TISS VPN
    @Value("${cilantro.bot.tiss.vpn.keypass: -1}")
    public String PRIVATE_TISS_VPN_KEYPASS;
    @Value("${cilantro.bot.tiss.vpn.keyalias: -1}")
    public String PRIVATE_TISS_VPN_KEY_ALIAS;
    @Value("${cilantro.bot.tiss.vpn.keypath: -1}")
    public String PRIVATE_TISS_VPN_KEY_FILE_PATH;
    @Value("${cilantro.bot.tiss.vpn.pfx.keypath:-1}")
    public String PRIVATE_TISS_VPN_PFX_KEY_FILE_PATH;
    @Value("${cilantro.bot.tiss.vpn.publicKeypass:-1}")
    public String PUBLIC_TISS_VPN_KEYPASS;
    @Value("${cilantro.bot.tiss.vpn.publicKeyalias:-1}")
    public String PUBLIC_TISS_VPN_KEY_ALIAS;
    @Value("${cilantro.bot.tiss.vpn.pubkeypath:-1}")
    public String PUBLIC_TISS_VPN_KEY_FILE_PATH;
    @Value("${cilantro.bcx.client.id:BCX-TCB90Hyqw@1i3}")
    public String BCX_CLIENT_ID;
    @Value("${cilantro.bcx.pan.decrypt.key:KpStjc%QVFj78tjN62nK?ZyKhM*r&0ahTwMzQ}")
    public String BCX_PAN_DECRYPT_KEY;
    @Value("${bot.is.tiss.vpn.allowed.for.branch:false}")
    public boolean IS_TISS_VPN_ALLOWED_FOR_BRANCH;
    @Value("${cilantro.amount.that.require.compliance:100000000}")
    public String AMOUNT_THAT_REQUIRES_COMPLIANCE;
    @Value("${kipayment.gepg.payment.url: -1}")
    public String GEPG_PAYMENT_MIDDLEWARE_URL;
    @Value("${airtel.namequery.url: -1}")
    public String AIRTEL_NAMEQUERY_URL;
    @Value("${airtel.trust.account.float.url: -1}")
    public String AIRTEL_TRUSTGATEWAY_URL;
    @Value("${kipayment.api.url: -1}")
    public String KIPAYMENT_API_URL;
    @Value("${kipayment.cms.namequery.url: -1}")
    public String KIPAYMENT_CMS_NAMEQUERY_URL;
    @Value("${kipayment.cms.namequery.url: -1}")
    public String MKOMBOZI_MIRROR_LEDGER;
    @Value("${cilantro.visa.card.issuance.sms.token:-1}")
    public String VISA_CARD_REGISTRATION_INITIAL_PIN;

    @Value("${kipayment.cashme.namequery.url: -1}")
    public String KIPAYMENT_CASHME_NAMEQUERY_URL;
    @Value("${kipayment.cashme.deposit_notify.url: -1}")
    public String KIPAYMENT_CASHME_DEPOSIT_NOTIFY_URL;

    @Value("${spring.eft.bot.tach.reversalcodes: AC01,AC04,AC06}")
    public String BOT_EFT_REVERSAL_CODES;
    @Value("${tips.LOOKUP.URL: -1}")
    public String TIPS_LOOKUP_URL;
    @Value("${tips.PAYMENT.URL: -1}")
    public String TIPS_PAYMENT_URL;
    @Value("${tips.CALLBACK_URL: -1}")
    public String TIPS_CALLBACK_URL;
    @Value("${tips.INITIATE_TXN_REVERSAL:-1}")
    public String INITIATE_TIPS_TXN_REVERSAL;
    @Value("${tips.REVERSAL_ON_WORKFLOW: -1}")
    public String TIPS_TXNS_ON_WF;
    @Value("${tips.BOT_TRANSFER_ENQUIRY:-1}")
    public String BOT_TRANSFER_ENQUIRY;
    @Value("${tips.VIKOBA_BOT_TRANSFER_ENQUIRY:-1}")
    public String VIKOBA_BOT_TRANSFER_ENQUIRY;
    @Value("${tips.CANCEL_TIPS_REVERSAL:-1}")
    public String CANCEL_TIPS_REVERSAL;
    @Value("${tips.TIPS_MAXIMUM_TRANSFER_LIMIT:5000000}")
    public String TIPS_MAXIMUM_TRANSFER_LIMIT;
    @Value("${tips.allowed:true}")
    public boolean IS_TIPS_ALLOWED;
    @Value("${tips.TRANSFER_REPORT: -1}")
    public String TIPS_TRANSER_REPORT_URL;
    @Value("${tips.FSP_PARTICIPANTS: -1}")
    public String FSP_PARTICIPANTS;
    @Value("${tips.AUTHORIZE_TIPS_REVERSAL: -1}")
    public String AUTHORIZE_TIPS_REVERSAL;
    @Value("${tips.REGISTER_TIPS_FRAUD: -1}")
    public String REGISTER_TIPS_FRAUD_URL;
    @Value("${tips.TCB_TIPS_FRAUDS: -1}")
    public String GET_TCB_TIPS_FRAUDS_URL;
    @Value("${tips.PRINT_ADVISE_URL: -1}")
    public String PRINT_TIPS_ADVISE_URL;

    @Value("${brinjal.api.url}")
    public String BRINJAL_API_URL;

    @Value("${channel.manager.api.url:http://172.25.2.228:3800/philae/xws/xapi}")
    public String CHANNEL_MANAGER_API_URL;

    @Value("${ubx.cert.private.key.path: -1}")
    public Resource UBX_PRIVATE_KEY_PATH;
    @Value("${ubx.cert.private.key.password:-1}")
    public String UBX_PRIVATE_KEY_PASSWORD;

    @Value("${ubx.cert.public.key.path: -1}")
    public Resource UBX_PUBLIC_KEY_PATH;
    @Value("${ubx.api.client.id: -1}")
    public String UBX_CLIENT_ID;
    @Value("${ubx.cert.password: -1}")
    public String UBX_CERT_PASSWORD;
    @Value("${ubx.user.id: -1}")
    public String UBX_USER_ID;
    @Value("${ubx.api.base.uri:-1}")
    public String UBX_API_BASE_URI;
    @Value("${instant.visa.message:Ndugu Mteja PIN yako ni: %s, Ibadilishe kwenye ATM za TCB Bank ndani ya Masaa 6 Kuanzia sasa}")
    public String INSTANT_VISA_MESSAGE;

    @Value("${CUSTOMER_SERVICE_CHARGE_BALANCE_ENQUIRY:1000}")
    public String CUSTOMER_SERVICE_CHARGE_BALANCE_ENQUIRY;

    @Value("${CUSTOMER_SERVICE_CHARGE_STATEMENT_PER_PAGE:2000}")
    public String CUSTOMER_SERVICE_CHARGE_STATEMENT_PER_PAGE;

    @Value("${CUSTOMER_SERVICE_CHARGE_INCOME_LEDGER:1-***-00-4016-4016007}")
    public String CUSTOMER_SERVICE_CHARGE_INCOME_LEDGER;

    @Value("${CUSTOMER_SERVICE_CHARGE_SCHEME:X03}")
    public String CUSTOMER_SERVICE_CHARGE_SCHEME;

    @Value("${CUSTOMER_SERVICE_CHARGE_CODE:CASHOUT}")
    public String CUSTOMER_SERVICE_CHARGE_CODE;


    @Value("${VISA_CARD_REQUEST_CHARGE:0}")
    public String VISA_CARD_REQUEST_CHARGE;

    @Value("${VISA_CARD_REQUEST_CHARGE_INCOME_LEDGER:1-***-00-4016-4016007}")
    public String VISA_CARD_REQUEST_CHARGE_INCOME_LEDGER;

    @Value("${VISA_CARD_REQUEST_CHARGE_SCHEME:X03}")
    public String VISA_CARD_REQUEST_CHARGE_SCHEME;

    @Value("${VISA_CARD_REQUEST_CHARGE_CODE:CASHOUT}")
    public String VISA_CARD_REQUEST_CHARGE_CODE;
    @Value("${tcb.stawi.bond.accounts:-1}")
    public List<String> TCB_STAWI_BOND_ACCOUNTS;
    @Value("${sensitive.data.encryption.key:GhJGY7ezGrX4BbJDsCvuw2uFqpxbquJh}")
    public String SENSITIVE_DATA_ENCRYPTION_KEY;
    @Value("${tcb.stawi.bond.notification.url:-1}")
    public String TCB_STAWI_BOND_NOTIFICATION_URL;
    @Value("${tcb.stawi.bond.lookup.url:-1}")
    public String TCB_STAWI_BOND_LOOKUP_URL;

    @Value("${AMOUNT_THAT_REQUIRES_BACKOFFICE_APPROVALS:100000000}")
    public String AMOUNT_THAT_REQUIRES_BACKOFFICE_APPROVALS;
    @Value("${MPESA_DO_CODES:WALLET2MKOBA,MKOBA2WALLET,M-PESA}")
    public String MPESA_DO_CODES;
    @Value("${general.RECON_SIZE: 2}")
    public Integer GENERAL_RECON_SIZE;
    @Value("${vikoba.RECON_SIZE: 3}")
    public Integer VIKOBA_GENERAL_RECON_SIZE;
    @Value("${loan.LOAN_SCORING_URL: -1}")
    public String loan_SCORING_ENGINE_URL;
    @Value("${loan.SCORED_CUSTOMER_LIMITS: -1}")
    public String loan_SCORED_CUSTOMER_LIMITS_URL;
    @Value("${loan.CUSTOMER_LOANS: -1}")
    public String loan_CUSTOMER_LOANS_URL;
    @Value("${loan.OUTSTANDING: -1}")
    public String loans_OUTSTANDINGLOANS_URL;
    @Value("${loan.PORTFOLIO_POSITION: -1}")
    public String loans_PORTFOLIO_POSITION_URL;
    @Value("${loan.outstandingLOAN_URL:-1}")
    public String outstandingLOAN_URL;
    @Value("${loan.REPAYMENT_SUMMARY_PER_ACCOUNT:-1}")
    public String loan_REPAYMENT_SUMMARY_PER_ACCOUNT_URL;
    @Value("${loan.REPAYMENT_SUMMARY_PER_LOAN:-1}")
    public String loan_REPAYMENT_SUMMARY_PER_LOAN_URL;
    @Value("${loan.LOAN_TYPES:-1}")
    public String loan_TYPES;
    @Value("${batch.callback.url: http://172.20.5.112:8080}")
    public String LOCAL_CALLBACK_URL;
    @Value("${spring.CREDIT_TRANSFER_AWAITING.LEDGER:1-070-00-2064-2064002}")
    public String CREDIT_TRANSFER_AWAITING_LEDGER;
    @Value("${muse_STATEMENT_URL:-1}")
    public String MUSE_STATEMENT_URL;
    @Value("${TPB_SMSC_GATEWAY_URL:-1}")
    public String TPB_SMSC_GATEWAY_URL;
    @Value("${pensioners_VERIFICATION_URL:-1}")
    public String PENSIONERS_VERIFICATION_URL;
    @Value("${pensioners_STATEMENT_URL:-1}")
    public String PENSIONERS_STATEMENT_URL;
    @Value("${pensioners_LOAN_VERIFICATION_URL:-1}")
    public String PENSIONERS_LOAN_VERIFICATION_URL;
    @Value("${brinjal.loanRepayment.url:-1}")
    public String BRINJAL_LOAN_REPAYMENT_URL;
    @Value("${brinjal.tanesco.saccoss.proxy.url:http://172.20.1.26:8081/processEft2Tanesco}")
    public String BRINJAL_TANESCO_SACCOSS_PROXY;
    @Value("${kipayment.TAUSI_AGENT_DETAILS.url:-1}")
    public String KIPAYMENT_TAUSI_AGENT_DETAILS_URL;

    @Value("${KIPAYMENT_TAUSI_NOTIFY_ACCOUNT_STATUS_URL:-1}")
    public String KIPAYMENT_TAUSI_NOTIFY_ACCOUNT_STATUS_URL;

    @Value("${KIPAYMENT_TAUSI_NOTIFY_AGENT_STATUS_URL:-1}")
    public String KIPAYMENT_TAUSI_NOTIFY_AGENT_STATUS_URL;

    @Value("${pensioners.CALLBACK_URL:-1}")
    public String PENSIONERS_CALLBACK_URL;

    @Value("${brinjal.tips.settlement.account:-1}")
    public String TIPS_SETTLEMENT_ACCOUNT;
/*
    BATCH CALLBACK FOR EFT PROCESSING
    */
     @Value("${eft.batch.callback.url: -1}")
    public String EFT_BATCH_CALLBACK_URL;

    @Value("${vikoba.operations.url:-1}")
    public String VIKOBA_OPERATIONS_URL;
    @Value("${payroll.batch.callback.url:-1}")
    public String PAYROLL_BATCH_CALLBACK_URL;

    @Value("${GePG_V2_KIPAYMENT_LOOKUP_URL:http://uat.tcbbank.co.tz:8443/api/gepg/v2/lookup_v_2}")
    public String GePG_V2_KIPAYMENT_LOOKUP_URL;

    @Value("${GePG_V2_KIPAYMENT_PAYMENT_URL:http://uat.tcbbank.co.tz:8443/api/gepg/v2/gepg-v2-payment}")
    public String GePG_V2_KIPAYMENT_PAYMENT_URL;

    @Value("${brinjal.nisogezeManualLoanRepayment.url: http://172.21.2.12:8444/wakala/esb/loan/insuarance/nitrogenizeManualLoanRepayment}")
    public String BRINJAL_NISOGEZE_REPAYMENT_URL;
    @Value("${zcsra.cert.private.key.path:-1}")
    public String ZCSRA_PRIVATE_KEY_PATH;
    @Value("${zcsra.cert.private.key.alias:-1}")
    public String ZCSRA_PRIVATE_KEY_ALIAS;
    @Value("${zcsra.cert.public.key.alias:-1}")
    public String ZCSRA_PUBLIC_KEY_ALIAS;
    @Value("${zcsra.cert.private.key.pass:-1}")
    public String ZCSRA_PRIVATE_KEY_PASS;
    @Value("${zcsra.cert.public.key.path:-1}")
    public String ZCSRA_PUBLIC_KEY_PATH;
    @Value("${zcsra.cert.public.key.pass:-1}")
    public String ZCSRA_PUBLIC_KEY_PASS;
    @Value("${zcsra.api.key:-1}")
    public String ZCSRA_API_KEY;
    @Value("${zcsra.api.base.url.demographic:-1}")
    public String ZCSRA_API_BASE_URL_DEMOGRAPHIC;
    @Value("${zcsra.api.base.url.biometric.verification:-1}")
    public String ZCSRA_API_BASE_URL_BIOMETRIC_VERIFICATION;
    @Value("${brela.ors.api.key:-1}")
    public String BRELA_ORS_API_KEY;

    @Value("${tcb.sms.notification.uri:http://172.20.1.26:8081/MMG/}")
    public String TCB_SMS_NOTIFICATION_URI;

    @Value("${brinjal.api.base.url}")
    public String BRINJAL_API_BASE_URL;

    @Value("${ACTIVE_MQ_API_BASE_URL:tcp://127.0.0.1:61666}")
    public String ACTIVE_MQ_API_BASE_URL;
    @Value("${VISA_CARD_MIN_AVAILABLE_BALANCE:0}")
    public String VISA_CARD_MIN_AVAILABLE_BALANCE;
//    KAFKA PARAMS CONFIG
    @Value("${spring.kafka.bootstrap-servers: localhost:9092}")
    public String KAFKA_BOOTSTRAP_SERVERS;
    @Value("${kafka.topic.activate-card:activate-card-topic}")
    public String KAFKA_TOPIC_ACTIVE_CARD_TOPIC;
    @Value("${kafka.topic.link-card:link-card-topic}")
    public String KAFKA_TOPIC_LINK_CARD_TOPIC;
    @Value("${kafka.topic.link-card.retry:link-card-retry-topic}")
    public String KAFKA_TOPIC_LINK_CARD_RETRY;
    @Value("${kafka.topic.reissue-pin:reissue-pin-topic}")
    public String KAFKA_TOPIC_REISSUE_PIN;
    @Value("${kafka.topic.reissue-retry:reissue-pin-retry-topic}")
    public String KAFKA_TOPIC_REISSUE_RETRY;
    @Value("${kafka.topic.reissue-pin:reissue-pin-topic}")
    public String KAFKA_TOPIC_REISSUE_TOPIC;
    @Value("${kafka.topic.pin-change:pin-change-topic}")
    public String KAFKA_TOPIC_PIN_CHANGE;
    @Value("${kafka.topic.reissue-retry:pin-change-retry-topic}")
    public String KAFKA_TOPIC_PIN_REISSUE_RETRY;
    @Value("${kafka.topic.notify:notification-topic}")
    public String KAFKA_TOPIC_NOTIFICATION;
    @Value("${kafka.topic.notify-retry:notification-retry-topic}")
    public String KAFKA_TOPIC_NOTIFICATION_RETRY;
    @Value("${kafka.topic.pin-change-retry:pin-change-retry-topic}")
    public String KAFKA_TOPIC_PIN_CHANGE_RETRY;


    private static final Logger LOGGER = LoggerFactory.getLogger(QueueProducer.class);

    public UsRole achUserRole(String branchCode) {
        List<Map<String, Object>> branchDetails;
        UsRole achRole = new UsRole();
        try {
            branchDetails = this.jdbcRUBIKONTemplate.queryForList("SELECT * FROM BUSINESS_UNIT bu WHERE bu.BU_CD =?", branchCode);
            if (!branchDetails.isEmpty()) {
                achRole.setLimits(achRole.getLimits());
                achRole.setDrawers(achRole.getDrawers());
                achRole.setBranchCode(branchCode);
                achRole.setBranchId(Long.valueOf(branchDetails.get(0).get("BU_ID").toString()));
                achRole.setBranchName(branchDetails.get(0).get("BU_NM").toString());
                achRole.setBuRoleId(Long.valueOf("5559"));
                achRole.setRole("Branch Supervisor");
                achRole.setRoleId(Long.valueOf("144"));
                achRole.setSupervisor("Y");
                achRole.setUserId(Long.valueOf("2755"));
                achRole.setUserName("STP002");
                achRole.setUserRoleId(Long.valueOf("31578"));

            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return achRole;
    }

    public philae.api.UsRole apiUserRole(String branchCode) {
        List<Map<String, Object>> branchDetails;
        philae.api.UsRole apiRole = new philae.api.UsRole();
        try {
            branchDetails = this.jdbcRUBIKONTemplate.queryForList("SELECT * FROM BUSINESS_UNIT bu WHERE bu.BU_CD =?", branchCode);
            if (!branchDetails.isEmpty()) {
                apiRole.setLimits(apiRole.getLimits());
                apiRole.setDrawers(apiRole.getDrawers());
                apiRole.setBranchCode(branchCode);
                apiRole.setBranchId(Long.valueOf(branchDetails.get(0).get("BU_ID").toString()));
                apiRole.setBranchName(branchDetails.get(0).get("BU_NM").toString());
                apiRole.setBuRoleId(Long.valueOf("5559"));
                apiRole.setRole("Branch Supervisor");
                apiRole.setRoleId(Long.valueOf("144"));
                apiRole.setSupervisor("Y");
                apiRole.setUserId(Long.valueOf("2755"));
                apiRole.setUserName("STP002");
                apiRole.setUserRoleId(Long.valueOf("31578"));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return apiRole;

    }

    public String getSysConfiguration(String name, String appProfile) {
        String res;
        try {
            String QUERY = null;
            if (appProfile == null) {
                System.out.println("APPLICATION_PROFILE: Default");
                QUERY = "SELECT DEV_VALUE AS DEV_VALUE AS VALUE FROM system_configuration WHERE NAME=? LIMIT 1";
            } else if (appProfile.equalsIgnoreCase("dev") || appProfile.equalsIgnoreCase("kajilo")) {
                System.out.println("APPLICATION_PROFILE: Development");
                QUERY = "SELECT DEV_VALUE  AS VALUE FROM system_configuration WHERE NAME=?";
            } else if (appProfile.equalsIgnoreCase("prod")) {
                System.out.println("APPLICATION_PROFILE: Production");
                QUERY = "SELECT PROD_VALUE AS VALUE FROM system_configuration WHERE NAME=?";
            } else if (appProfile.equalsIgnoreCase("dr")) {
                System.out.println("APPLICATION_PROFILE: DR Site");
                QUERY = "SELECT DR_VALUE AS VALUE FROM system_configuration WHERE NAME=?";
            }
            res = jdbcTemplate.queryForObject(QUERY, new Object[]{name}, String.class);
            LOGGER.info("getSysConfiguration :RESULT: {} ", res);

        } catch (Exception e) {
            LOGGER.info(e.getMessage());
            res = "-1";
        }
        return res;
    }

    public static String generateTransactionReference(int n) {
        // chose a Character random from this String
        String AlphaNumericString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ" + "0123456789" + "abcdefghijklmnopqrstuvxyz";
        // create StringBuffer size of AlphaNumericString
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) {
            // generate a random number between
            // 0 to AlphaNumericString variable length
            int index = (int) (AlphaNumericString.length() * Math.random());
            // add Character one by one in end of sb
            sb.append(AlphaNumericString.charAt(index));
        }
        return sb.toString();
    }

    public String getDealNumberGenerated() {
        // It will generate 6 digit random Number.
        // from 0 to 999999
        Random rnd = new Random();
        int number = rnd.nextInt(999999);
        // this will convert any number sequence into 6 character.
        return String.format("%06d", number);
    }
}
