function getCoreBankingRates() {
    $("#loading").show();
    var accountNo = $("input[name='senderAccount']").val();
    var accountName = $("input[name='accountName']").val();
    var sendingCurrency = $("select[name='currency']").val();
    var accountCurrency = $("input[name='accountCurrency']").val();
    if (accountNo.length !== 0 && sendingCurrency.length !== 0 && accountName.length !== 0) {
        $.ajax({
            url: "/getCoreBankingRates",
            type: 'GET',
            data: {sendingCurrency: sendingCurrency, accountNo: accountNo},
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
                    if (accountCurrency !== 'TZS' && sendingCurrency !== 'TZS') {
                        //DEBIT SHOULD BE FROM THAT CURRENCY TO TZS
                        $("#debitRateType").val("(" + accountCurrency + " TO TZS)");
                        $("#creditRateType").val("(TZS TO " + sendingCurrency + ")");
                    }
                    if (accountCurrency == 'TZS' || sendingCurrency != 'TZS') {
                        //DEBIT SHOULD BE FROM THAT CURRENCY TO TZS
                        $("#debitRateType").text("(" + accountCurrency + " TO " + sendingCurrency);
                        $("#creditRateType").text("(" + accountCurrency + " TO " + sendingCurrency + ")");
                    }
                    $("#rubikonDebitRate").val(res[0].DEBITRATE);
                    $("#rubikonCreditRate").val(res[0].CREDITRATE);
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

function initiateDealCreation() {
    $("#loading").show();
    var formdata = new FormData($('#specialRateDealNumber')[0]);
    $.ajax({
        url: "/initiateSpecialRateDealNumberGeneration",
        type: 'POST',
        data: formdata,
        enctype: 'multipart/form-data',
        processData: false,
        contentType: false,
        success: function (res) {
            $("#loading").hide();
            $('.error').html('');
            $('#profilesInitiated').DataTable().ajax.reload();
            if (res.validated) {
                $('#preloader2').hide();
                $('#rtgsTransferFormDiv').hide();
                $(function () {
                    setTimeout(function () {
                        showElement();
                    }, 3000);

                    function showElement() {
                        $("#successFlag").html('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + res.jsonString + '</strong></div>');
                    }
                });
            } else {
                $('#preloader2').hide();
                $("#successFlag").html('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + res.jsonString + '</strong></div>');
                $.each(res.errorMessages, function (key, value) {
                    $('Select[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');
                    $('input[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');
                    $('textarea[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');

                });
            }
        },
        error: function (res) {
            $('.create-ib-client-profile').attr('disabled', 'disabled');
            $("#loading").hide();
            $('.error').html('');
            if (res.validated) {
                $.each(res.errorMessages, function (key, value) {
                    $('input[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');
                    $('Select[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');
                    $('textarea[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');

                });
            } else {
                if (typeof res.responseJSON.errors !== 'undefined') {
                    $.each(res.responseJSON.errors, function (key, value) {
                        $('input[name=' + value.field + ']').after('<span class="error" style="color:red">' + value.defaultMessage + '</span>');
                        $('Select[name=' + value.field + ']').after('<span class="error" style="color:red">' + value.defaultMessage + '</span>');
                        $('textarea[name=' + value.field + ']').after('<span class="error" style="color:red">' + value.defaultMessage + '</span>');

                    });
                } else {
                    $('#errorMessage').after('<div class="row"><h4 class="error" style="color:red"> Error: ' + res.responseJSON.message + '</h4></div>');
                }
            }
        }
    });
}

function previewDealNumberForApproval(txnid, conversionCurrency, systemRate, requestedRate, fxType) {
    $("#loading").show();
    $.ajax({
        url: "/previewDealNumberForApproval",
        data: {reference: txnid, conversionCurrency: conversionCurrency, systemRate: systemRate, requestedRate: requestedRate, fxType: fxType},
        type: 'POST',
        success: function (response) {
            $("#loading").hide();
            $("#myModal").modal("show");
            $("#areaValue").html(response);
        }
    });
}
function approveDealNumber(txnid) {
    $("#loading").show();
    $.ajax({
        url: "/approveDealNumber",
        data: {reference: txnid, status: "approved"},
        type: 'POST',
        success: function (response) {
            $("#loading").hide();
            $("#messageData").hide();
            if (response.result == 0) {
                $("#successFlag").after('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + '</strong></div>');
            } else {
                $("#successFlag").after('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + response.result + '</strong></div>');
            }
//            $('#successFlag').delay(5000).show();
            reloadDatatable("#pendingDealnumbers");
        }
    });
}
function rejectDealNumber(txnid) {
    $("#loading").show();
    $.ajax({
        url: "/approveDealNumber",
        data: {reference: txnid, status: "reject"},
        type: 'POST',
        success: function (response) {
            $("#loading").hide();
            $("#messageData").hide();
            if (response.result == 0) {
                $("#successFlag").after('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + '</strong></div>');
            } else {
                $("#successFlag").after('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + response.result + '</strong></div>');
            }
//            $('#successFlag').delay(5000).show();
            reloadDatatable("#pendingDealnumbers");
        }
    });
}