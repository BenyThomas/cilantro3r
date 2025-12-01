$(document).ready(function () {
    /*
     * TELLER INITIATE RTGS PAYMENTS
     */
    $('.initiate-rtgs-payments').click(function (e) {
        $('.initiate-rtgs-payments').attr('disabled', 'disabled');
        $("#loading").show();
        var formdata = new FormData($('#rtgsTransferForm')[0]);
        $.ajax({
            url: "/initiateRTGSRemittance",
            type: 'POST',
            data: formdata, //{senderAccount: senderAccount, senderName: senderName, senderAddress: senderAddress, amount: amount, currency: currency, beneficiaryName: beneficiaryName, beneficiaryBIC: beneficiaryBIC, description: description, supporting_doc: supporting_doc, beneficiaryAccount: beneficiaryAccount},
            enctype: 'multipart/form-data',
            processData: false,
            contentType: false,
            success: function (res) {

                $("#loading").hide();
                $('.error').html('');
                if (res.validated) {
                    $('#preloader2').hide();
                    $('#rtgsTransferFormDiv').hide();
                    $("#successFlag").html('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + res.jsonString + '</strong></div>');
                } else {
                    $('#preloader2').hide();
                    $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + res.jsonString + '</strong></div>');
                    $.each(res.errorMessages, function (key, value) {
                        $('select[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');
                        $('input[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');
                        $('textarea[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');
                    });
                }
            },
            error: function (res) {
                console.log(res);
                $(".initiate-rtgs-payments").removeAttr("disabled");
                $("#loading").hide();
                $('.error').html('');
                if (res.validated) {
                    $.each(res.errorMessages, function (key, value) {
                        $('input[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');
                        $('select[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');
                        $('textarea[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');
                    });
                } else {
                    if (typeof res.responseJSON.errors !== 'undefined') {
                        $.each(res.responseJSON.errors, function (key, value) {
                            $('input[name=' + value.field + ']').after('<span class="error" style="color:red">' + value.defaultMessage + '</span>');
                            $('select[name=' + value.field + ']').after('<span class="error" style="color:red">' + value.defaultMessage + '</span>');
                            $('textarea[name=' + value.field + ']').after('<span class="error" style="color:red">' + value.defaultMessage + '</span>');
                        });
                    } else {
                        $('#errorMessage').after('<div class="row"><h4 class="error" style="color:red"> Error: ' + JSON.stringify(res) + '</h4></div>');
                     $.each(res.errorMessages, function (key, value) {
                        $('select[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');
                        $('input[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');
                        $('textarea[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');
                    });
                    }
                }
            }
        });
    });
       /*
     * TELLER INITIATE GePG RTGS PAYMENTS
     */
    $('.initiate-gepg-rtgs-payments').click(function (e) {
        $('.initiate-gepg-rtgs-payments').attr('disabled', 'disabled');
        $("#loading").show();
        var formdata = new FormData($('#rtgsTransferForm')[0]);
        $.ajax({
            url: "/initiateRTGSRemittance",
            type: 'POST',
            data: formdata, //{senderAccount: senderAccount, senderName: senderName, senderAddress: senderAddress, amount: amount, currency: currency, beneficiaryName: beneficiaryName, beneficiaryBIC: beneficiaryBIC, description: description, supporting_doc: supporting_doc, beneficiaryAccount: beneficiaryAccount},
            enctype: 'multipart/form-data',
            processData: false,
            contentType: false,
            success: function (res) {

                $("#loading").hide();
                $('.error').html('');
                if (res.validated) {
                    $('#preloader2').hide();
                    $('#rtgsTransferFormDiv').hide();
                    $("#successFlag").html('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + res.jsonString + '</strong></div>');
                } else {
                    $('#preloader2').hide();
                    $("#successFlag").html('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + res.jsonString + '</strong></div>');
                    $.each(res.errorMessages, function (key, value) {
                        $('select[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');
                        $('input[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');
                        $('textarea[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');
                    });
                }
            },
            error: function (res) {
                $(".initiate-gepg-rtgs-payments").removeAttr("disabled");
                $("#loading").hide();
                $('.error').html('');
                if (res.validated) {
                    $.each(res.errorMessages, function (key, value) {
                        $('input[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');
                        $('select[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');
                        $('textarea[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');
                    });
                } else {
                    if (typeof res.responseJSON.errors !== 'undefined') {
                        $.each(res.responseJSON.errors, function (key, value) {
                            $('input[name=' + value.field + ']').after('<span class="error" style="color:red">' + value.defaultMessage + '</span>');
                            $('select[name=' + value.field + ']').after('<span class="error" style="color:red">' + value.defaultMessage + '</span>');
                            $('textarea[name=' + value.field + ']').after('<span class="error" style="color:red">' + value.defaultMessage + '</span>');
                        });
                    } else {
                        $('#errorMessage').after('<div class="row"><h4 class="error" style="color:red"> Error: ' + res.responseJSON.message + '</h4></div>');
                    }
                }
            }
        });
    });
    /*
     * TELLER INITIATE EFT PAYMENTS
     */
    $('.initiate-eft-bulk-payments').click(function (e) {
        $('initiate-eft-bulk-payments').attr('disabled', 'disabled');
        $("#loading").show();
        var formdata = new FormData($('#bulkPayments')[0]);
        $.ajax({
            url: "/initiateEftBulkPayments",
            type: 'POST',
            data: formdata, //{senderAccount: senderAccount, senderName: senderName, senderAddress: senderAddress, amount: amount, currency: currency, beneficiaryName: beneficiaryName, beneficiaryBIC: beneficiaryBIC, description: description, supporting_doc: supporting_doc, beneficiaryAccount: beneficiaryAccount},
            enctype: 'multipart/form-data',
            processData: false,
            contentType: false,
            success: function (res) {
                console.log(res);
                $("#loading").hide();
                $('.error').html('');
                if (res.validated) {
                    $('#preloader2').hide();
                    $('#rtgsTransferFormDiv').hide();
                    $("#successFlag").html('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + res.message + '</strong></div>');
                } else {
                    $('#preloader2').hide();
                    $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + res.message + '</strong></div>');
                    $.each(res.errorMessages, function (key, value) {
                        $('select[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');
                        $('input[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');
                        $('textarea[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');
                    });
                }
            },
            error: function (res) {
                $(".initiate-rtgs-payments").removeAttr("disabled");
                $("#loading").hide();
                $('.error').html('');
                if (res.validated) {
                    $.each(res.errorMessages, function (key, value) {
                        $('input[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');
                        $('select[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');
                        $('textarea[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');
                    });
                } else {
                    if (typeof res.responseJSON.errors !== 'undefined') {
                        $.each(res.responseJSON.errors, function (key, value) {
                            $('input[name=' + value.field + ']').after('<span class="error" style="color:red">' + value.defaultMessage + '</span>');
                            $('select[name=' + value.field + ']').after('<span class="error" style="color:red">' + value.defaultMessage + '</span>');
                            $('textarea[name=' + value.field + ']').after('<span class="error" style="color:red">' + value.defaultMessage + '</span>');
                        });
                    } else {
                        $('#errorMessage').after('<div class="row"><h4 class="error" style="color:red"> Error: ' + res.responseJSON.message + '</h4></div>');
                    }
                }
            }
        });
    });
    /*
     * FINANCE INITIATE RTGS PAYMENTS TO SERVICE PROVIDERS
     */
    $('.initiate-rtgs-payments-finance').click(function (e) {
        var hasVAT = false;
        if ($("#vat").is(':checked')) hasVAT = true;
        $('.initiate-rtgs-payments-finance').attr('disabled', 'disabled');
        $("#loading").show();
        var formdata = new FormData($('#rtgsTransferFormFinance')[0]);
        formdata.set('vat', hasVAT);
        $.ajax({
            url: "/financeInitiateRTGSRemittance",
            type: 'POST',
            data: formdata, //{senderAccount: senderAccount, senderName: senderName, senderAddress: senderAddress, amount: amount, currency: currency, beneficiaryName: beneficiaryName, beneficiaryBIC: beneficiaryBIC, description: description, supporting_doc: supporting_doc, beneficiaryAccount: beneficiaryAccount},
            enctype: 'multipart/form-data',
            processData: false,
            contentType: false,
            success: function (res) {

                $("#loading").hide();
                $('.error').html('');
                if (res.validated) {
                    $('#preloader2').hide();
                    $('#rtgsTransferFormDiv').hide();
                    $("#successFlag").html('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>Transaction is successfully submitted to Cashier Workflow approvals</strong></div>');
                } else {
                    $('#errorMessage').after('<div class="row"><h4 class="error" style="color:red"> Error: ' + res.jsonString + '</h4></div>');
                    $('#preloader2').hide();
                    $.each(res.errorMessages, function (key, value) {
                        $('select[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');
                        $('input[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');
                        $('textarea[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');
                    });
                }
            },
            error: function (res) {
                $(".initiate-rtgs-payments").removeAttr("disabled");
                $("#loading").hide();
                $('.error').html('');
                if (res.validated) {
                    $.each(res.errorMessages, function (key, value) {
                        $('input[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');
                        $('select[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');
                        $('textarea[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');
                    });
                } else {
                    if (typeof res.responseJSON.errors !== 'undefined') {
                        $.each(res.responseJSON.errors, function (key, value) {
                            $('input[name=' + value.field + ']').after('<span class="error" style="color:red">' + value.defaultMessage + '</span>');
                            $('select[name=' + value.field + ']').after('<span class="error" style="color:red">' + value.defaultMessage + '</span>');
                            $('textarea[name=' + value.field + ']').after('<span class="error" style="color:red">' + value.defaultMessage + '</span>');
                        });
                    } else {
                        $('#errorMessage').after('<div class="row"><h4 class="error" style="color:red"> Error: ' + res.responseJSON.message + '</h4></div>');
                    }
                }
            }
        });
    });
    /*
     * RETURN TRANSACTION FOR AMMENDMEND  
     */
    $('.return-rtgs-for-amendment').click(function (e) {
        $('.return-rtgs-for-amendment').attr('disabled', 'disabled');
        $("#loading").show();
        var formdata = new FormData($('#returnForAmmendmend')[0]);
        $.ajax({
            url: "/returnRTGSForAmmendmend",
            data: formdata,
            type: 'POST',
            enctype: 'multipart/form-data',
            processData: false,
            contentType: false,
            success: function (response) {
                $("#preloader2").hide();
                $("#loading").hide();
                if (response.result == 1) {
                    $("#successFlag").html('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + '</strong></div>').show();
                } else {
                    $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + response.result + '</strong></div>').show();
                }
                $('#successFlag').delay(2000).show();
                reloadDatatable("#rtgsTxnOnWorkFlow");
                reloadDatatable("#approveBranchRemittance");
            }
        });
    });

    /*
     * REJECT RTGS TRANSACTION
     */
    $('.reject-rtgs-transaction').click(function (e) {
        $('.reject-rtgs-transaction').attr('disabled', 'disabled');
        $("#loading").show();
        var formdata = new FormData($('#rejectRtgsTransaction')[0]);
        $.ajax({
            url: "/fireRejectRTGSTransaction",
            data: formdata,
            type: 'POST',
            enctype: 'multipart/form-data',
            processData: false,
            contentType: false,
            success: function (response) {
                $("#preloader2").hide();
                $("#loading").hide();
                if (response.result == 1) {
                    $("#successFlag").html('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + '</strong></div>').show();
                } else {
                    $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + response.result + '</strong></div>').show();
                }
                $('#successFlag').delay(2000).show();
                reloadDatatable("#rtgsTxnOnWorkFlow");
                reloadDatatable("#approveBranchRemittance");
            }
        });
    });
    /*
     * AMMEND RTGS TRANSACTION 
     */
    $('.ammend-rtgs-payments').click(function (e) {
        $('.ammend-rtgs-payments').attr('disabled', 'disabled');
        $("#loading").show();
        var formdata = new FormData($('#ammendRTGSTransaction')[0]);
        $.ajax({
            url: "/amendRTGSTransaction",
            type: 'POST',
            data: formdata, //{senderAccount: senderAccount, senderName: senderName, senderAddress: senderAddress, amount: amount, currency: currency, beneficiaryName: beneficiaryName, beneficiaryBIC: beneficiaryBIC, description: description, supporting_doc: supporting_doc, beneficiaryAccount: beneficiaryAccount},
            enctype: 'multipart/form-data',
            processData: false,
            contentType: false,
            success: function (res) {
                $("#loading").hide();
                $('.error').html('');
                reloadDatatable("#rtgsTxnOnWorkFlow");
                if (res.validated) {
                    $('#preloader2').hide();
                    $('#rtgsTransferFormDiv').hide();
                    $("#successFlag").html('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>Transaction is successfully submitted to Cashier Workflow approvals</strong></div>');
                } else {
                    $('#preloader2').hide();
                    $.each(res.errorMessages, function (key, value) {
                        $('select[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');
                        $('input[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');
                        $('textarea[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');
                    });
                }
            },
            error: function (res) {
                $(".initiate-rtgs-payments").removeAttr("disabled");
                $("#loading").hide();
                $('.error').html('');
                if (res.validated) {
                    $.each(res.errorMessages, function (key, value) {
                        $('input[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');
                        $('select[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');
                        $('textarea[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');
                    });
                } else {
                    if (typeof res.responseJSON.errors !== 'undefined') {
                        $.each(res.responseJSON.errors, function (key, value) {
                            $('input[name=' + value.field + ']').after('<span class="error" style="color:red">' + value.defaultMessage + '</span>');
                            $('select[name=' + value.field + ']').after('<span class="error" style="color:red">' + value.defaultMessage + '</span>');
                            $('textarea[name=' + value.field + ']').after('<span class="error" style="color:red">' + value.defaultMessage + '</span>');
                        });
                    } else {
                        $('#errorMessage').after('<div class="row"><h4 class="error" style="color:red"> Error: ' + res.responseJSON.message + '</h4></div>');
                    }
                }
            }
        });
    });
    //authorize payments
    $('#approvePayments').click(function () {
        $('#initiatingGePGTxns').show();
        var references = "'0'";
        var post_arr = [];
        console.log('here then');
        $('#approveGePGRemittance input[type=checkbox]').each(function () {
            if (jQuery(this).is(":checked")) {
                var id = this.id;
                var datas = id.split('==');
                references += ",'" + datas[0] + "'";
                post_arr.push({
                    sourceAcct: datas[0]
                });
            }
        });
        if (post_arr.length > 0) {
            $.ajax({
                url: "/approveGePGMNORemittance",
                type: 'POST',
                data: {references: references},
                success: function (response) {
                    console.log(response);
                    $('#initiatingGePGTxns').hide();
                    $("#initiateGepgRemittance").hide();
                    $("#gepgAccountsBalances").hide();
                    $("#successFlag").show();
                    if (response.result == 0) {
                        $("#successFlag").after('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + '</strong></div>');
                    } else {
                        $("#successFlag").after('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + '</strong></div>');
                    }
                    console.log(response);
                }
            });
        } else {
            alert('No data selected');
        }
    });
});
/*
 * Branch chief cashier preview swift message and authorize/cancel/return for amendment
 */
function previewSwiftMsg(txnid) {
    $("#loading").show();
    $.ajax({
        url: "/previewSwiftMsg",
        data: {reference: txnid},
        type: 'POST',
        success: function (response) {
            $("#loading").hide();
            $("#myModal").modal("show");
            $("#areaValue").html(response);
        }
    });
}

/*
 * Finance Approver preview swift message and authorize/cancel/return for amendment
 */
function financePreviewSwiftMsg(txnid) {
    $("#loading").show();
    $.ajax({
        url: "/financePreviewSwiftMsgAndAuthorize",
        data: {reference: txnid},
        type: 'POST',
        success: function (response) {
            $("#loading").hide();
            $("#myModal").modal("show");
            $("#areaValue").html(response);
        }
    });
}
/*
 * AUTHORIZER PREVIEW SWIFT MESSAGE AND AUTHORIZE THE PAYMENTS
 */
function ibdPreviewSwiftMsg(txnid) {
    $("#loading").show();
    $.ajax({
        url: "/previewSwiftMsgAndAuthorize",
        data: {reference: txnid},
        type: 'POST',
        success: function (response) {
            $("#loading").hide();
            $("#myModal").modal("show");
            $("#areaValue").html(response);
        }
    });
}
/*
 * 
 * @param {type} txnid
 * PREVIEW SWIFT MESSAGE AND SUPPORTING DOCUMENT ON REPORTS previewMsgAndDocs
 */
function previewMsgAndDocs(txnid) {
    $("#loading").show();
    $.ajax({
        url: "/previewSwiftMsgAndDocReport",
        data: {reference: txnid},
        type: 'POST',
        success: function (response) {
            $("#loading").hide();
            $("#myModal").modal("show");
            $("#areaValue").html(response);
        }
    });
}
/*
 * AMEND THE TRANSACTION ON THE QUEUE.
 */
function amendRtgsTransaction(txnid) {
    $("#loading").show();
    $('.select2').select2({
        dropdownParent: $('#myModal')
    });
    $.ajax({
        url: "/getRTGSTransactionForAmendmend",
        data: {reference: txnid},
        type: 'GET',
        success: function (response) {

            $("#loading").hide();
            $("#myModal").modal("show");
            $("#areaValue").html(response);
        }
    });
}
function previewSupportingDocuments(txnid) {
    $("#loading").show();
    $.ajax({
        url: "/previewSupportingDocuments",
        data: {reference: txnid},
        type: 'POST',
        success: function (response) {
            $(".authorize-payments").removeAttr("disabled");
            $("#loading").hide();
            $("#myModal").modal("show");
            $("#areaValue").html(response);
        }
    });
}
/*
 * 
 * @param {type} txnid
 * Branch chief supervisor/Branch operational manager authorizes the payments by debiting customer account and credit TA
 */
function authorizeRTGSBranch(txnid) {
    $('#authorizeIBDPyaments').attr('disabled', 'disabled');
    $('#cancelPayments').attr('disabled', 'disabled');
    $('#returnForamendment').attr('disabled', 'disabled');
    $("#loading").show();
    $.ajax({
        url: "/authorizeRTGSonWorkFlow",
        data: {reference: txnid},
        type: 'POST',
        success: function (response) {
            $("#loading").hide();
            $("#messageData").hide();
            if (response.result == 0) {
                $("#successFlag").after('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + '</strong></div>');
            } else {
                $("#successFlag").after('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + response.result + '</strong></div>');
            }
            $('#successFlag').delay(2000).show();
            reloadDatatable("#approveBranchRemittance");
        }
    });
}
/*
 * 
 * @param {type} txnid
 * International Banking department (IBD) Approve the payments to be remitted (debit TA and credit BOT Nostro account)
 */
function ibdApproveRTGS(txnid) {
    $('#authorizeIBDPyaments').attr('disabled', 'disabled');
    $('#cancelPayments').attr('disabled', 'disabled');
    $('#returnForamendment').attr('disabled', 'disabled');
    $("#loading").show();
    $("#messageData").hide();
    $("#preloader2").show();
    $.ajax({
        url: "/approveBranchRemittance",
        data: {reference: txnid},
        type: 'POST',
        success: function (response) {
            $("#preloader2").hide();
            $("#loading").hide();
            if (response.result == 0) {
                $("#successFlag").html('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + '</strong></div>').show();
            } else {
                $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + response.result + '</strong></div>').show();
            }
            $('#successFlag').delay(2000).show();
            reloadDatatable("#approveBranchRemittance");
        }
    });
}
/*
 * 
 * @param {type} txnid
 * FINANCE APPROVE RTGS TRANSACTION ON THE WORK-FLOW
 */
function financeApproveRTGS(txnid) {
    $('#authorizeIBDPyaments').attr('disabled', 'disabled');
    $('#cancelPayments').attr('disabled', 'disabled');
    $('#returnForamendment').attr('disabled', 'disabled');
    $("#loading").show();
    $("#messageData").hide();
    $("#preloader2").show();
    $.ajax({
        url: "/approveFinanceRTGSRemittance",
        data: {reference: txnid},
        type: 'POST',
        success: function (response) {
            $("#preloader2").hide();
            $("#loading").hide();
            if (response.result == 0) {
                $("#successFlag").html('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + '</strong></div>').show();
            } else {
                $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + response.result + '</strong></div>').show();
            }
            $('#successFlag').delay(2000).show();
            reloadDatatable("#approveBranchRemittance");
        }
    });
}
/*
 * Return the transaction for amendment (BEFORE POSTING THE TRANSACTIONS) i.e before Debiting customer account and crediting TA 
 */
function branchSupervisorReturnForAmmendmend(txnid) {
    $('#authorizeIBDPyaments').attr('disabled', 'disabled');
    $('#cancelPayments').attr('disabled', 'disabled');
    $('#returnForamendment').attr('disabled', 'disabled');
    $("#loading").show();
    $("#messageData").hide();
    $("#preloader2").show();
    $.ajax({
        url: "/returnRTGSForAmmendmend",
        data: {reference: txnid},
        type: 'POST',
        success: function (response) {
            $("#preloader2").hide();
            $("#loading").hide();
            if (response.result == 0) {
                $("#successFlag").html('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + '</strong></div>').show();
            } else {
                $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + response.result + '</strong></div>').show();
            }
            $('#successFlag').delay(2000).show();
            reloadDatatable("#approveBranchRemittance");
        }
    });
}
/*
 * IBD Return the transactions for amendment TO BRANCH LEVEL (NO REVERSAL REQUIRED)
 */
function IbdReturnForAmmendmendToBranch(txnid) {
    $('#authorizeIBDPyaments').attr('disabled', 'disabled');
    $('#cancelPayments').attr('disabled', 'disabled');
    $('#returnForamendment').attr('disabled', 'disabled');
    $("#loading").show();
    $("#messageData").hide();
    $("#preloader2").show();
    $.ajax({
        url: "/approveBranchRemittance",
        data: {reference: txnid},
        type: 'POST',
        success: function (response) {
            $("#preloader2").hide();
            $("#loading").hide();
            if (response.result == 1) {
                $("#successFlag").html('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + '</strong></div>').show();
            } else {
                $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + response.result + '</strong></div>').show();
            }
            $('#successFlag').delay(2000).show();
            reloadDatatable("#approveBranchRemittance");
        }
    });
}
/*
 * Preview EFT BULK PAYMENTS 
 */
function previewEftBulkPayments(batchReference, batchAmount) {
    $("#loading").show();
    $.ajax({
        url: "/previewEftBulkPayments",
        data: {batchReference: batchReference, batchAmount: batchAmount},
        type: 'POST',
        success: function (response) {
            $("#loading").hide();
            $("#myModal").modal("show");
            $("#areaValue").html(response);
        }
    });
}
/*
 * PPAROVE EFT BATCH TRANSACTION
 */

function branchApproveEftBatchTxn(txnid, batchCount) {
    $('#authorizeEftBatchTxn').attr('disabled', 'disabled');
    $('#cancelPayments').attr('disabled', 'disabled');
    $('#returnBatchForamendment').attr('disabled', 'disabled');
    $("#loading").show();
    $.ajax({
        url: "/approveEftBatchBulkPayment",
        data: {reference: txnid, batchCount: batchCount},
        type: 'POST',
        success: function (response) {
            $("#loading").hide();
            $("#messageData").hide();
            if (response.result == 0) {
                $("#successFlag").after('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + '</strong></div>');
            } else {
                $("#successFlag").after('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + response.result + '</strong></div>');
            }
            $('#successFlag').delay(2000).show();
            reloadDatatable("#eftBulkPaymentsOnWorkFlow");
        }
    });
}
/*
 * Preview HQ EFT BULK PAYMENTS 
 */
function hqPreviewEftBulkPayments(batchReference, batchAmount) {
    $("#loading").show();
    $.ajax({
        url: "/hqPreviewEftBatchPayments",
        data: {batchReference: batchReference, batchAmount: batchAmount},
        type: 'POST',
        success: function (response) {
            $("#loading").hide();
            $("#myModal").modal("show");
            $("#areaValue").html(response);
        }
    });
}
/*
 * HQ PPAROVE EFT BATCH TRANSACTION
 */

function hqApproveEftBatchTxn(txnid, batchCount) {
    $('#authorizeEftBatchTxn').attr('disabled', 'disabled');
    $('#cancelPayments').attr('disabled', 'disabled');
    $('#returnBatchForamendment').attr('disabled', 'disabled');
    $("#loading").show();
    $.ajax({
        url: "/approveBranchEftBatchPayment",
        data: {reference: txnid, batchCount: batchCount},
        type: 'POST',
        success: function (response) {
            $("#loading").hide();
            $("#messageData").hide();
            if (response.result == 0) {
                $("#successFlag").after('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + '</strong></div>');
            } else {
                $("#successFlag").after('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + response.result + '</strong></div>');
            }
            $('#successFlag').delay(2000).show();
            reloadDatatable("#eftBulkPaymentsOnWorkFlow");
        }
    });
}

function hqDiscardEftBatchTxn(txnid, batchCount) {
    $('#authorizeEftBatchTxn').attr('disabled', 'disabled');
    $('#cancelPayments').attr('disabled', 'disabled');
    $('#returnBatchForamendment').attr('disabled', 'disabled');
    $("#loading").show();
    $.ajax({
        url: "/fireHqDiscardEftBatchTxn",
        data: {reference: txnid, batchCount: batchCount},
        type: 'POST',
        success: function (response) {
            $("#loading").hide();
            $("#messageData").hide();
            if (response.result == 0) {
                $("#successFlag").after('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + '</strong></div>');
            } else {
                $("#successFlag").after('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + response.result + '</strong></div>');
            }
            $('#successFlag').delay(2000).show();
            reloadDatatable("#eftBulkPaymentsOnWorkFlow");
        }
    });
}
/*
 * PREVIEW INWARD EFT SUCCESSFULLY TRANSACTIONS 
 */

function previewEftSuccessTxns(senderBic, txndate, fromTime, toTime) {
    $("#loading").show();
    $.ajax({
        url: "/previewEftPerBankTxns",
        data: {senderBic: senderBic, txnStatus: 'C', txndate: txndate, fromTime: fromTime, toTime: toTime},
        type: 'POST',
        success: function (response) {
            $("#loading").hide();
            $("#myModal").modal("show");
            $("#areaValue").html(response);
        }
    });
}
/*
 * PREVIEW INWARD EFT SUCCESSFULLY TRANSACTIONS 
 */

function previewEftFailedTxns(senderBic, txndate, fromTime, toTime) {
    $("#loading").show();
    $.ajax({
        url: "/previewEftPerBankTxns",
        data: {senderBic: senderBic, txnStatus: 'F', txndate: txndate, fromTime: fromTime, toTime: toTime},
        type: 'POST',
        success: function (response) {
            $("#loading").hide();
            $("#myModal").modal("show");
            $("#areaValue").html(response);
        }
    });
}
/*
 * PREVIEW INWARD EFT SUCCESSFULLY TRANSACTIONS 
 */

function previewTxnInQueue(senderBic, txndate, fromTime, toTime) {
    $("#loading").show();
    $.ajax({
        url: "/previewEftPerBankTxns",
        data: {senderBic: senderBic, txnStatus: 'QI', txndate: txndate, fromTime: fromTime, toTime: toTime},
        type: 'POST',
        success: function (response) {
            $("#loading").hide();
            $("#myModal").modal("show");
            $("#areaValue").html(response);
        }
    });
}
function reloadDatatable(datatableName) {
    $("#loading").hide();
    $(datatableName).DataTable().ajax.reload();
}
function addCommas(nStr)
{
    nStr += '';
    x = nStr.split('.');
    x1 = x[0];
    x2 = x.length > 1 ? '.' + x[1] : '';
    var rgx = /(\d+)(\d{3})/;
    while (rgx.test(x1)) {
        x1 = x1.replace(rgx, '$1' + ',' + '$2');
    }
    return x1 + x2;
}
function getExchangeRateFrmCoreBanking() {
    $("#loading").show();
    var accountNo = $("input[name='senderAccount']").val();
    var accountName = $("input[name='senderName']").val();
    var sendingCurrency = $("select[name='currency']").val();
    var accountCurrency = $("input[name='accountCurrency']").val();
    if (accountNo.length != 0 && sendingCurrency.length != 0 && accountName.length != 0) {
        $.ajax({
            url: "/coreBankingExchangeRate",
            type: 'GET',
            data: {sendingCurrency: sendingCurrency, accountNo: accountNo, accountCurrency: accountCurrency},
            dataType: "json",
            success: function (res) {
                //ON CLICK SPECIAL RATE BUTTON DISPLAY COLUMNS
                $("#systemRate").show();
                $("#requestingRate").show();
                $("#loading").hide();
                $("#rubikonRate").val(res[0].RATE);
                $("#currencyConversion").val(accountCurrency + ' TO ' + sendingCurrency)
                $("#fxType").val(res[0].FXTYPE);
                console.log(res)
            },
            error: function (res) {
                $("#loading").hide();
                $('#senderAccount').after('<h4 class="error" style="color:red     font-size: 8px !important;"> Error: Please Try Again. Please contact IT Helpdesk / Database Administrator: RUBIKON NOT FOUND</h4>');
                console.log(res);
            }
        });
    } else {
        $("#loading").hide();
        $("#successFlag").after('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>Please enter Sender account Number and select the sending currency</strong></div>');
    }
}
function corebankingEchangeRates() {
    $("#loading").show();
    var accountNo = $("input[name='senderAccount']").val();
    var accountName = $("input[name='senderName']").val();
    var sendingCurrency = $("select[name='currency']").val();
    var accountCurrency = $("input[name='accountCurrency']").val();
    if (accountNo.length != 0 && sendingCurrency.length != 0 && accountName.length != 0) {
        $.ajax({
            url: "/coreBankingExchangeRate",
            type: 'GET',
            data: {sendingCurrency: sendingCurrency, accountNo: accountNo, accountCurrency: accountCurrency},
            dataType: "json",
            success: function (res) {
                if (accountCurrency == sendingCurrency) {
                      $("#loading").hide();
                    $("#successFlag").after('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>Sending Currency is the same as Account currency Special rate is not required</strong></div>');
                } else {
                    //ON CLICK SPECIAL RATE BUTTON DISPLAY COLUMNS
                    $("#systemRate").show();
                    $("#requestingRate").show();
                    $("#loading").hide();
                    if (accountCurrency != 'TZS' && sendingCurrency != 'TZS') {
                        //DEBIT SHOULD BE FROM THAT CURRENCY TO TZS
                        $("#debitRateType").val("(" + accountCurrency + " TO TZS)");
                        $("#creditRateType").val("(TZS TO " + sendingCurrency + ")");
                    }
                    if (accountCurrency != 'TZS' || sendingCurrency != 'TZS') {
                        //DEBIT SHOULD BE FROM THAT CURRENCY TO TZS
                        $("#debitRateType").text("(" + accountCurrency + " TO " + sendingCurrency);
                        $("#creditRateType").text("(" + sendingCurrency + " TO " + accountCurrency + ")");
                    }
                    $("#rubikonDebitRate").val(res[0].RATE);
                    $("#rubikonCreditRate").val(res[0].RATE);
                    $("#currencyConversion").val(accountCurrency + ' TO ' + sendingCurrency)
                    $("#fxType").val(res[0].FXTYPE);
                    console.log(res)
                }
            },
            error: function (res) {
                $("#loading").hide();
                $('#senderAccount').after('<h4 class="error" style="color:red     font-size: 8px !important;"> Error: Please Try Again. Please contact IT Helpdesk / Database Administrator: RUBIKON NOT FOUND</h4>');
                console.log(res);
            }
        });
    } else {
        $("#loading").hide();
        $("#successFlag").after('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>Please enter Sender account Number and select the sending currency</strong></div>');
    }
}
/*
 * Branch chief cashier preview swift message and authorize/cancel/return for amendment
 */
function previewSpecialRateTxn(txnid, conversionCurrency, systemRate, requestedRate, fxType) {
    $("#loading").show();
    $.ajax({
        url: "/previewSpecialRateTxn",
        data: {reference: txnid, conversionCurrency: conversionCurrency, systemRate: systemRate, requestedRate: requestedRate, fxType: fxType},
        type: 'POST',
        success: function (response) {
            $("#loading").hide();
            $("#myModal").modal("show");
            $("#areaValue").html(response);
        }
    });
}
/*
 * 
 * @param {type} txnid
 * Branch chief supervisor/Branch operational manager authorizes the payments by debiting customer account and credit TA
 */
function reverseTransaction(txnid) {
    $('#authorizeIBDPyaments').attr('disabled', 'disabled');
    $('#cancelPayments').attr('disabled', 'disabled');
    $('#returnForamendment').attr('disabled', 'disabled');
    $("#loading").show();
    $.ajax({
        url: "/reverseRejectRtgsPayment",
        data: {reference: txnid},
        type: 'POST',
        success: function (response) {
            $("#loading").hide();
            $("#messageData").hide();
            if (response.result == 0) {
                $("#successFlag").after('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + '</strong></div>');
            } else {
                $("#successFlag").after('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + response.result + '</strong></div>');
            }
            $('#successFlag').delay(2000).show();
            reloadDatatable("#approveBranchRemittance");
        }
    });
}

function generateSwiftFile(txnid) {
    $('#authorizeIBDPyaments').attr('disabled', 'disabled');
    $('#cancelPayments').attr('disabled', 'disabled');
    $('#returnForamendment').attr('disabled', 'disabled');
    $("#loading").show();
    $.ajax({
        url: "/redumpPayment",
        data: {reference: txnid},
        type: 'POST',
        success: function (response) {
            $("#loading").hide();
            $("#messageData").hide();
            if (response.result == 0) {
                $("#successFlag").after('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + '</strong></div>');
            } else {
                $("#successFlag").after('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + response.result + '</strong></div>');
            }
            $('#successFlag').delay(2000).show();
            reloadDatatable("#approveBranchRemittance");
        }
    });
}
function selectAll() {
    var items = document.getElementsByName('txndetails');
    for (var i = 0; i < items.length; i++) {
        if (items[i].type === 'checkbox')
            items[i].checked = true;
    }
}
function UnSelectAll() {
    var items = document.getElementsByName('txndetails');
    for (var i = 0; i < items.length; i++) {
        if (items[i].type === 'checkbox')
            items[i].checked = false;
    }
}
function returnInwardEftTransactions() {
    var post_arr = [];
    var references = "'0'";
    // sending confirmation email for transactions not in RUBIKON
    $('#inwardEFTBankSuccessSummary input[type=checkbox]').each(function () {
        if (jQuery(this).is(":checked")) {
            var id = this.id;
            var datas = id.split('==');
            references += ",'" + datas[0] + "'";
            post_arr.push({
                txnid: datas[0]
            });
        }
    });
    if (post_arr.length > 0) {
        $("#loading").modal("show");
        $.ajax({
            url: "/returnInwardEftTransactions",
            type: 'POST',
            data: {references: references},
            success: function (response) {
                $("#loading").hide();
                if (response.result == 0) {
                    $("#successFlag").after('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + '</strong></div>');
                } else {
                    $("#successFlag").after('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + response.result + '</strong></div>');
                }

            }
        });
    } else {
        alert('No Entry Selected!!!!!');
    }
}
/*
 * PREVIEW INWARD RTGS SUCCESSFULLY TRANSACTIONS 
 */

function previewRTGSInwardSuccessTxns(senderBic,fromDate, toDate1, toDate) {
    $("#loading").show();
    $.ajax({
        url: "/previewRtgsIncomingTransactionsPerBank",
        data: {senderBic: senderBic, txnStatus: 'C', fromDate: fromDate, toDate: toDate1},
        type: 'POST',
        success: function (response) {
            $("#loading").hide();
            $("#myModal").modal("show");
            $("#areaValue").html(response);
        }
    });
}
/*
 * PREVIEW INWARD EFT SUCCESSFULLY TRANSACTIONS 
 */

function previewRTGSInwardFailedTxns(senderBic, fromDate, toDate1, toDate) {
    $("#loading").show();
    $.ajax({
        url: "/previewRtgsIncomingTransactionsPerBank",
        data: {senderBic: senderBic, txnStatus: 'F', fromDate: fromDate, toDate: toDate1},
        type: 'POST',
        success: function (response) {
            $("#loading").hide();
            $("#myModal").modal("show");
            $("#areaValue").html(response);
        }
    });
}


/**KWAMUA THINGS BEGIN*/

/*Authorize Mobile movement transaction to PV**/
function initiateKprinterTxn(reference,swift_message,currency){
//$('#initiateKprinterTxnBtn').prop('disabled','disabled');
    var reason = $("#reason").val();
    if(reason != ''){
        $("#loading").show();
         $.ajax({
             url: "/fireInitiateKprinterTxnToWF",
             data: {reference: reference,swift_message: swift_message, reason: reason ,currency: currency},
             type: 'POST',
             success: function (response) {
                         $("#loading").hide();
                         $("#messageData").hide();
                         if (response.result == 0) {
                             $("#successFlag").html('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + '</strong></div>');
                             $('#initiateKprinterTxnBtn').prop('disabled','disabled');
                         } else {
                             $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + response.result + '</strong></div>');
                         }
                         $('#successFlag').delay(2000).show();
                     }
         });
      }else{
           $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong> Please provide reason </strong></div>');
      }
}


/*
 *
 * SM DPSS authorizes the cash movement
 */
function previewPendingTXNWF(reference,swift_message, currency) {

    $("#loading").show();
    $.ajax({
        url: "/firePreviewPendingSwiftTXN",
        data: {reference: reference, swift_message: swift_message, currency: currency},
        type: 'POST',
        success: function (response) {
            $("#loading").hide();
            $("#myModal").modal("show");
            $("#areaValue").html(response);
        }
    });
}

  /*Authorize Mobile movement transaction to PV**/
    function fireAuthorizeSwiftTxnToKprinter(reference,swift_message){
    $('#fireAuthorizeSwiftTxn').prop('disabled','disabled');
     $("#loading").show();
         $.ajax({
             url: "/fireAuthorizeSwiftTxnToKprinter",
             data: {reference: reference, swift_message: swift_message},
             type: 'POST',
             success: function (response) {
                         $("#loading").hide();
                         $("#messageData").hide();
                         if (response.result == 0) {
                             $("#successFlag").html('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + '</strong></div>');
                             $('#transactionSummary').DataTable().ajax.reload();
                         } else {
                             $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message  + '</strong></div>');
                         }
                         $('#successFlag').delay(2000).show();
                     }
         });
    }


    /*Return visa card for ammendment**/
function returnVisaCardForAmmendment(reference) {
    $('#approveCardRequestBranch').attr('disabled', 'disabled');
    $('#returnVisaCardForAmmendment').attr('disabled', 'disabled');
    $("#loading").show();
    $.ajax({
        url: "/fireReturnVisaCardForAmmendment",
        data: {reference: reference},
        type: 'POST',
        success: function (response) {
            $("#loading").hide();
            $("#messageData").hide();
            if (response.result == 0) {
                $("#successFlag").after('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + '</strong></div>');
            } else {
                $("#successFlag").after('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + response.result + '</strong></div>');
            }
            $('#successFlag').delay(2000).show();
            reloadDatatable("#profilesInitiated");
        }
    });
}

function cancelEftOnHQWF(batch_reference) {
    $.ajax({
        url: "/fireCancelEftHQWFAjax",
        data: {batch_reference: batch_reference},
        type: 'POST',
        success: function (response) {
             console.log(response.responseCode);
            if (response.responseCode == 0) {
                iziToast.success({ title: 'SUCCESS', message: 'Transaction Removed From Workflow', color:'green',position:'topRight', timeout: 3500 });
                 reloadDatatable("#dtable1");
            } else {
                iziToast.error({ title: 'FAIL', message: 'Failed To Remove Txn From WF', color:'red',position:'topRight', timeout: 5000 });
            }
        }
    });
}

var th = ['', 'thousand', 'million', 'billion', 'trillion'];

var dg = ['zero', 'one', 'two', 'three', 'four', 'five', 'six', 'seven', 'eight', 'nine'];

var tn = ['ten', 'eleven', 'twelve', 'thirteen', 'fourteen', 'fifteen', 'sixteen', 'seventeen', 'eighteen', 'nineteen'];

var tw = ['twenty', 'thirty', 'forty', 'fifty', 'sixty', 'seventy', 'eighty', 'ninety'];

function number2words(s) {
    s = s.toString();
    s = s.replace(/[\, ]/g, '');
    if (s != parseFloat(s)) return 'not a number';
    var x = s.indexOf('.');
    if (x == -1) x = s.length;
    if (x > 15) return 'too big';
    var n = s.split('');
    var str = '';
    var sk = 0;
    for (var i = 0; i < x; i++) {
        if ((x - i) % 3 == 2) {
            if (n[i] == '1') {
                str += tn[Number(n[i + 1])] + ' ';
                i++;
                sk = 1;
            } else if (n[i] != 0) {
                str += tw[n[i] - 2] + ' ';
                sk = 1;
            }
        } else if (n[i] != 0) {
            str += dg[n[i]] + ' ';
            if ((x - i) % 3 == 0) str += 'hundred ';
            sk = 1;
        }
        if ((x - i) % 3 == 1) {
            if (sk) str += th[(x - i - 1) / 3] + ' ';
            sk = 0;
        }
    }
    if (x != s.length) {
        var y = s.length;
        str += 'point ';
        for (var i = x + 1; i < y; i++) str += dg[n[i]] + ' ';
    }
    return str.replace(/\s+/g, ' ');

}

/*
     * FINANCE INITIATE MULTIPLE GL DEBITS RTGS PAYMENTS TO SERVICE PROVIDERS
     */
    $('#initiate_multiple_gl_transaction_finance').click(function (e) {
//        var hasVAT = false;
//        if ($("#vat").is(':checked')) hasVAT = true;
        var lastCount = $('#theLastItemIndex').val();
        $('#initiate_multiple_gl_transaction_finance').attr('disabled', 'disabled');
        $("#loading").show();
//        var serialData = $('#financeMultipleGLPosting').serialize();
        var itemSize = $('#theLastItemIndex').val();
        var itemArray = [];
        for(var i=1; i<=itemSize; i++){
        var item = {};
            item["glAcctNo"] = $('#senderAccount'+i).val();
            item["amount"] = $('#amount'+i).val();
            item["vat"] = $('#vat'+i).is(':checked') ? 1 : 0;
            itemArray.push(item);
        }
        var formdata = new FormData($('#financeMultipleGLPosting')[0]);
            formdata.append('itemArray',JSON.stringify(itemArray));
        $.ajax({
            url: "/fireInitiateMultipleRTGSRemittance",
            type: 'POST',
            data: formdata,
            enctype: 'multipart/form-data',
            processData: false,
            contentType: false,
            success: function (res) {

                $("#loading").hide();
                $('.error').html('');
                if (res.status === "SUCCESS") {
                    $('#preloader2').hide();
                    $('#parentLedgerDiv').hide();
                    $('#newRow').hide();
                    $("#successFlag").html('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>'+res.result+'</strong></div>');
                } else if(res.status === "ERROR"){
                    $('#preloader2').hide();
                    $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>'+res.result+'</strong></div>');
                }
                else {
                    $('#initiate_multiple_gl_transaction_finance').prop('disabled', 'false');
                    $('#errorMessage').after('<div class="row"><h4 class="error" style="color:red"> Error: ' + res.jsonString + '</h4></div>');
                    $('#preloader2').hide();
                    $.each(res.errorMessages, function (key, value) {
                        $('[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');
                    });
                }
            },
            error: function (res) {
                $(".initiate-rtgs-payments").removeAttr("disabled");
                $("#loading").hide();
                $('.error').html('');
                if (res.validated) {
                    $.each(res.errorMessages, function (key, value) {
                        $('input[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');
                        $('select[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');
                        $('textarea[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');
                    });
                } else {
                    if (typeof res.responseJSON.errors !== 'undefined') {
                        $.each(res.responseJSON.errors, function (key, value) {
                            $('input[name=' + value.field + ']').after('<span class="error" style="color:red">' + value.defaultMessage + '</span>');
                            $('select[name=' + value.field + ']').after('<span class="error" style="color:red">' + value.defaultMessage + '</span>');
                            $('textarea[name=' + value.field + ']').after('<span class="error" style="color:red">' + value.defaultMessage + '</span>');
                        });
                    } else {
                        $('#errorMessage').after('<div class="row"><h4 class="error" style="color:red"> Error: ' + res.responseJSON.message + '</h4></div>');
                    }
                }
            }
        });
    });


