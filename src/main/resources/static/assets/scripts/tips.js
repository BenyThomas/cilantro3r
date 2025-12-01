$(document).ready(function () {
    /*
     * TELLER INITIATE RTGS PAYMENTS
     */
    $('.initiate-tips-payments').click(function (e) {
        $('.initiate-tips-payments').attr('disabled', 'disabled');
        $("#loading").show();
        var formdata = new FormData($('#tipsTransferForm')[0]);
        $.ajax({
            url: "/submitTipsTransfer",
            type: 'POST',
            data: formdata,
            enctype: 'multipart/form-data',
            processData: false,
            contentType: false,
            success: function (res) {

                $("#loading").hide();
                $('.error').html('');
                if (res.validated) {
                    $('#preloader2').hide();
                    $('#tipsTransferForm').hide();
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
                $(".initiate-tips-payments").removeAttr("disabled");
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
function previewTIPSTransaction(reference) {
    $("#loading").show();
    $.ajax({
        url: "/previewTipsSwiftMsg",
        data: {reference: reference},
        type: 'POST',
        success: function (response) {
            $("#loading").hide();
            $("#myModal").modal("show");
            $("#areaValue").html(response);
        }
    });
}

/** confirm tips transaction status from BOT*/
function confirmBOT(reference) {
    $("#loading").show();
    $.ajax({
        url: "/confirmTxnStatusOnBOT",
        data: {reference: reference},
        type: 'POST',
        success: function (response) {
            $("#loading").hide();
            $("#myModal").modal("show");
            $("#areaValue").html(response);
        }
    });
}

/** Reverse TIPS Transaction */
function initiateTipsTxnReversal(reference) {
    $("#loading").show();
    $.ajax({
        url: "/initiateTipsTxnReversal",
        data: {reference: reference},
        type: 'POST',
        success: function (response) {
        console.log("script logged response: ....",response);
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
 *
 * @param {type} txnid
 * Branch chief supervisor/Branch operational manager authorizes the payments by debiting customer account and credit TA
 */
function authorizeTIPSBranch(reference) {
    $('#authorizeIBDPyaments').attr('disabled', 'disabled');
    $('#cancelPayments').attr('disabled', 'disabled');
    $('#returnForamendment').attr('disabled', 'disabled');
    $("#loading").show();
    $.ajax({
        url: "/authorizeTIPSonWorkFlow",
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
            reloadDatatable("#approveBranchRemittance");
        }
    });
}


/**
*reverse tips txn
*/

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


/** validate tips transaction for reversal*/
function getTipsTransactionDetails(){
        // get the form values
    var token = $("meta[name='_csrf']").attr("content");
    var header = $("meta[name='_csrf_header']").attr("content");
    var formdata = new FormData($('#tipsTransferForm')[0]);
    $.ajax({
       processData: false,
       contentType: false,
       data: formdata,
       type: "POST",
       url: "/getTipsTransactionDetails",
       beforeSend: function (request) {
            request.setRequestHeader(header, token);
       },
        success: function (response) {
                if((response.status === 'SUCCESS') && response.data !=null){
                    $("#loading").hide();
                    $(".tipsSuccessDependant").show();
                 }else{
                    alert("Failed to get transaction details using provided reference");
                 }

                 },
      error: function (e) {
                    console.log(e);
                  }
    });
}


/***Initiate tips transfer reversal**/
function initiateTipsTransferReversal(reference){
           // get the form values
           var token = $("meta[name='_csrf']").attr("content");
           var header = $("meta[name='_csrf_header']").attr("content");
           $("#initiateTipsTransferReversal").prop("disabled",true);
               var formdata = new FormData($('#tipsTxnReversalForm')[0]);
                formdata.append('reverseTipsTxnReference', reference);

//           console.log(formdata);
           $.ajax({
              processData: false,
              contentType: false,
              data: formdata,
              type: "POST",
              url: "/initiateTipsTransferReversal",
              beforeSend: function (request) {
                   request.setRequestHeader(header, token);
              },
               success: function (response) {
                       if(response){
                       var d = JSON.parse(response);
                           $("#loading").hide();
                                    if(d.responseCode == 0){
                                        $("#successFlag").after('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + d.message + ': Code: ' + d.responseCode +  '</strong></div>');
                                     } else {
                                         $("#successFlag").after('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + d.message + ': Code: ' + d.responseCode + '</strong></div>');
                                      }
                        }else{
                           alert("GENERAL FAILURE");
                        }

                        },
             error: function (e) {
                           console.log(e);
                         }
           });
}

/** Reverse TIPS transaction */
function reverseRequestedTipsTransaction(bankReference,reversalRef) {
//    $("#loading").show();
//    $('#authorizeRequestedReversal').attr('disabled', 'disabled');

    $.ajax({
        url: "/reverseRequestedTipsTransaction",
        data: {bankReference: bankReference, reversalRef: reversalRef},
        type: 'POST',
        success: function (response) {
        console.log(response);
            $("#loading").hide();
            $("#myModal").modal("show");
            $("#areaValue").html(response);
        }
    });
}

/**AUTHORIZE REVERSAL OF REQUESTED TIPS TRANSACTION */
function tipsAuthorizeRequestedReversal(bankReference,reversalRef) {
    $("#loading").show();
    $.ajax({
        url: "/tipsAuthorizeRequestedReversal",
        data: {bankReference: bankReference, reversalRef: reversalRef},
        type: 'POST',
        success: function (response) {
            $("#loading").hide();
            var d = JSON.parse(response);
                    console.log(d);

            if((d.responseCode == 0)){
                $("#successFlag").after('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + d.message + ': Code: ' + d.responseCode +  '</strong></div>');
            } else {
                $("#successFlag").after('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + d.message + ': Code: ' + d.responseCode + '</strong></div>');
            }
                $('#successFlag').delay(2000).show();

        }
    });
}


/** CANCEL TIPS REVERSAL*/
function transferReversalCancellation(bankReference,reversalRef) {
//    $("#loading").show();
//    $('#authorizeRequestedReversal').attr('disabled', 'disabled');

    $.ajax({
        url: "/fireCancelTipsTxnReversal",
        data: {bankReference: bankReference, reversalRef: reversalRef},
        type: 'POST',
        success: function (response) {
        console.log(response);
            $("#loading").hide();
            $("#myModal").modal("show");
            $("#areaValue").html(response);
        }
    });
}


/**AUTHORIZE CANCELLATION OF REQUESTED TIPS TRANSACTION REVERSAL */
function fireAuthorizeReversalCancellation(bankReference,reversalRef) {
    $("#loading").show();
    $.ajax({
        url: "/fireAuthorizeReversalCancellation",
        data: {bankReference: bankReference, reversalRef: reversalRef},
        type: 'POST',
        success: function (response) {
            $("#loading").hide();
            var d = JSON.parse(response);
                    console.log(d);

            if((d.responseCode == 0)){
                $("#successFlag").after('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + d.message + ': Code: ' + d.responseCode +  '</strong></div>');
            } else {
                $("#successFlag").after('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + d.message + ': Code: ' + d.responseCode + '</strong></div>');
            }
                $('#successFlag').delay(2000).show();

        }
    });
}


/**TIPS FRAUD REGISTRATION*/
   function registerFraud(){
            //get form data
              var token = $("meta[name='_csrf']").attr("content");
              var header = $("meta[name='_csrf_header']").attr("content");
              var formdata = new FormData($('#register-fraud-form')[0]);
               $.ajax({
                    url: "/registerTipsFraud",
                    type: 'POST',
                    data: formdata,
                    enctype: 'multipart/form-data',
                    processData: false,
                    contentType: false,
                    success: function (response) {
//                                     console.log(response);
                                     $('.error').html('');
                                if(response.status === "SUCCESS"){
                                    $("#successFlag").html('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>'+ response.data +'</strong></div>');

                                    $('.error').html(''); //clear error
                                    $("#form").trigger("reset");//reset form
                                    $('#another-element').hide();
                                }else if(response.status === "ERROR"){
                                    $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>'+ response.data +'</strong></div>');
                                }
                                else{
                                 $.each(response.data, function (key, value) {
                                    $('[name=' + key + ']').after('<span class="error text-danger">' + value + '</span>');
                                });

                                }
                            },
                    error: function(error){
                        console.log(error);
                    }
               });

            }

function validateMissingTipsTrans(){
      var reference= $('#reference').val();
      if(reference == ''){
      alert("Transaction reference can not be empty");
      }else{
      $('#validateTipsBtn').prop('disabled', true);
         $("#preloader2").show();
            $.ajax({
                url: '/resolveMissingTipsTransactionsAjax',
                type: "POST",
                data: {reference,reference},
                success: function (response) {
                 $("#preloader2").hide();
                  $("#customAlert").show();
                  console.log(response);
                  if(response.status === 'SUCCESS_REVERSAL'){
                     $("#customAlert").html('<div class="col-md-12 alert alert-success"  style="font-size:10px;  font-weight: bold;"><strong>' + '  ' + response.result  + '  ' + '</strong></div>');
                  }else{
                     $("#customAlert").html('<div class="col-md-12 alert alert-warning"  style="font-size:10px;  font-weight: bold;"><strong>' + '  ' + response.result  + '  ' + '</strong></div>');
                  }
                },
                error: function (e) {
                    $("#preloader2").hide();
                    console.log(e);
                }
            });
      }
}