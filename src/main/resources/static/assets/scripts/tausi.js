$(document).ready(function () {
    $("#transactionAmt").keyup(function (event) {
                 var keyInAmount = $(this).val();
                // skip for arrow keys
                if (event.which >= 37 && event.which <= 40) {
                    event.preventDefault();
                }

                $(this).val(function (index, value) {
                    value = value.replace(/,/g, '');
                    return numberWithCommas(value);
                });

             if(keyInAmount.length > 1){
                  $('#num2wordsDiv').html(number2words(keyInAmount));
             }
            });
});
function numberWithCommas(x) {
            var parts = x.toString().split(".");
            parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ",");
            return parts.join(".");
}

 function openNotifyTausiMforAcctStatus(agentCode, agentReference,transactionId) {
            iziToast.success({ title: 'PROCESSING', message: 'Please wait', color:'green',position:'topRight', timeout: 5000 });
           $.ajax({
               url: "/fireTausiAccountDetailsModal",
               data: {agentCode: agentCode,agentReference:agentReference,transactionId:transactionId},
            type: 'POST',
                success: function (response) {
                   $("#loading").hide();
                    $("#myModal").modal("show");
                   $("#areaValue").html(response);
                }
           });
    }

    function notifyAgentAccountStatus(agentCode, agentReference,transactionId){
        $('#notifyAcctStatus').prop('disabled',true);
           $.ajax({
               url: "/fireNotifyTausiAcctStatus",
               data: {agentCode: agentCode,agentReference:agentReference,transactionId:transactionId},
            type: 'POST',
                success: function (response) {
                   console.log(response);
                   var result = JSON.parse(response);
                   if(result.statusCode === '21000'){
                      iziToast.success({ title: 'SUCCESS', message: 'Notification completed successfully', color:'green',position:'topRight', timeout: 5000 });
                       $("#successFlag").html('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>Notification completed successfully</strong></div>');
                   }else{
                      iziToast.error({ title: 'ERROR', message: 'Notification Failed', color:'red',position:'topRight', timeout: 5000 });
                      $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>Failed to notify Tausi</strong></div>');
                   }
                   $('#mappedAgentAccounts').DataTable().ajax.reload();
                }
           });
    }

    function agentAccountDeposit(agentCode, agentReference,transactionId){
<!--     iziToast.success({ title: 'PROCESSING', message: 'Please wait', color:'green',position:'topRight', timeout: 5000 });-->
           $.ajax({
               url: "/fireTausiAccountDepositModal",
               data: {agentCode: agentCode,agentReference:agentReference,transactionId:transactionId},
            type: 'POST',
                success: function (response) {
                   $("#loading").hide();
                    $("#myModal").modal("show");
                   $("#areaValue").html(response);
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

function initiateAgentTransaction(agentCode,agentReference,agentAccount){
    var amount = $("#transactionAmt").val();
    var depositorName = $("#depositorName").val();
    if(amount != ''){
     $('#initiateAgentTxn').prop('disabled',true);
               $.ajax({
//                   url: "/fireInitiateAgentAccount",
                   url: "/fireInitiateAgentAccountByTeller",
                   data: {agentCode: agentCode,agentReference:agentReference,agentAccount:agentAccount, amount:amount,depositorName:depositorName},
                   type: 'POST',
                    success: function (response) {
                        var jsonObj = JSON.parse(response);
                        console.log(jsonObj);

                        if(jsonObj.status === '200'){
                          iziToast.success({ title: 'SUCCESS', message: 'Transaction initiated successfully', color:'green',position:'topRight', timeout: 5000 });
                          $("#successFlag").html('<div class="alert alert-success" role="alert" ><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>'+'Response Code '+jsonObj.status + ' Response Message [ ' + jsonObj.message +' ]'+'</strong></div>');
                       }else{
                          iziToast.error({ title: 'ERROR', message: jsonObj.message, color:'red',position:'topRight', timeout: 5000 });
                         $("#successFlag").html('<div class="alert alert-danger" role="alert" ><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>'+'Response Code '+jsonObj.status + ' Response Message [ ' + jsonObj.message +' ]'+'</strong></div>');
                       }
                     $('#mappedAgentAccounts').DataTable().ajax.reload();
                    }
               });
    }else{
       iziToast.error({ title: 'ERROR', message: 'Amount is required', color:'red',position:'topRight', timeout: 5000 });
    }
}

function previewTransactionB4Approve(agentCode,agentReference,transRef){
    $.ajax({
           url: "/firePreviewAgentTransaction",
           data: {agentCode: agentCode,agentReference:agentReference,transRef:transRef},
           type: 'POST',
                success: function (response) {
                $("#loading").hide();
                $("#myModal").modal("show");
                $("#areaValue").html(response);
              }
    });
}

function approveAgentTransaction(agentCode,agentReference,transRef){
    $("#approveBtn").prop('disabled',true);
              iziToast.success({ title: 'PROCESSING', message: 'Please Wait', color:'green',position:'topRight', timeout: 5000 });
               $.ajax({
                   url: "/fireApproveAgentTransaction",
                   data: {agentCode: agentCode,agentReference:agentReference,transRef:transRef},
                   type: 'POST',
                    success: function (response) {
                       console.log(response);
                       if(response.result === '200'){
                          iziToast.success({ title: 'SUCCESS', message: response.message, color:'green',position:'topRight', timeout: 5000 });
                          $("#successFlag").html('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>'+response.message+'</strong></div>');
                       }else{
                          iziToast.error({ title: 'ERROR', message: response.message, color:'red',position:'topRight', timeout: 6000 });
                        $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>'+response.message+'</strong></div>');
                       }
                      $('#agentTransactionsDT').DataTable().ajax.reload();
                    }
               });

}


function previewASettlementB4Approve(agentCode,agentReference,transRef,controlNo){
       $("#loading").show();
    $.ajax({
           url: "/firePreviewASettlementB4Approve",
           data: {agentCode: agentCode,agentReference:agentReference,transRef:transRef,controlNo:controlNo},
           type: 'POST',
                success: function (response) {
                $("#loading").hide();
                $("#myModal").modal("show");
                $("#areaValue").html(response);
              }
    });
}


function approveSettlementAgentTransaction(agentCode,agentReference,transRef){
$("#loading").show();
    $("#approveBtn").prop('disabled',true);
              iziToast.success({ title: 'PROCESSING', message: 'Please Wait', color:'green',position:'topRight', timeout: 5000 });
               $.ajax({
                   url: "/fireApproveSettlementAgentTransaction",
                   data: {agentCode: agentCode,agentReference:agentReference,transRef:transRef},
                   type: 'POST',
                    success: function (response) {
                    $("#loading").hide();
                       console.log(response);
                       if(response.result === '200' || response.result === '26'){
                          iziToast.success({ title: 'SUCCESS', message: response.message, color:'green',position:'topRight', timeout: 5000 });
                          $("#successFlag").html('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>'+response.status+'</strong></div>');
                       }else{
                          iziToast.error({ title: 'ERROR', message: response.message, color:'red',position:'topRight', timeout: 6000 });
                         $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>'+response.status+'</strong></div>');
                       }
//                      $('#agentTransactionSettDT').DataTable().ajax.reload();
                    }
               });

}


function previewNotifyAgentDeposit(agentCode,agentReference,transRef){
  $.ajax({
           url: "/firePreviewNotifyAgentDeposie",
           data: {agentCode: agentCode,agentReference:agentReference,transRef:transRef},
           type: 'POST',
                success: function (response) {
                $("#loading").hide();
                $("#myModal").modal("show");
                $("#areaValue").html(response);
              }
    });
}


function approveAgentTransactionNotification(agentCode,agentReference,transRef){
    $("#approveBtn").prop('disabled',true);
    $("#loading").show();
    iziToast.success({ title: 'PROCESSING', message: 'Please Wait', color:'green',position:'topRight', timeout: 5000 });
     $.ajax({
         url: "/fireApproveAgentTransactionNotify",
         data: {agentCode: agentCode,agentReference:agentReference,transRef:transRef},
         type: 'POST',
          success: function (response) {
          $("#loading").hide();
             console.log(response);
             if(response.result === '21000'){
                iziToast.success({ title: 'SUCCESS', message: response.message, color:'green',position:'topRight', timeout: 5000 });
               $("#successFlag").html('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>'+response.message+'</strong></div>');
             }else{
                iziToast.error({ title: 'ERROR', message: response.message, color:'red',position:'topRight', timeout: 6000 });
               $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>'+response.message+'</strong></div>');
             }
             $('#agentTransactionsDT').DataTable().ajax.reload();
          }
    });
 }

 function previewCancelMisPosted(agentCode,agentReference,transRef){
   $.ajax({
            url: "/firePreviewCancelMispostedTransaction",
            data: {agentCode: agentCode,agentReference:agentReference,transRef:transRef},
            type: 'POST',
                 success: function (response) {
                 $("#loading").hide();
                 $("#myModal").modal("show");
                 $("#areaValue").html(response);
               }
     });
 }

 function approveCancelMispostedTransaction(agentCode,agentReference,transRef){
     $("#approveBtn").prop('disabled',true);
     $("#loading").show();
     iziToast.success({ title: 'PROCESSING', message: 'Please Wait', color:'green',position:'topRight', timeout: 5000 });
      $.ajax({
          url: "/fireApproveCancelMispostedTransaction",
          data: {agentCode: agentCode,agentReference:agentReference,transRef:transRef},
          type: 'POST',
           success: function (response) {
           $("#loading").hide();
              console.log(response);
              if(response.result === '200'){
                 iziToast.success({ title: 'SUCCESS', message: response.message, color:'green',position:'topRight', timeout: 5000 });
                $("#successFlag").html('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>'+response.message+'</strong></div>');
              }else{
                 iziToast.error({ title: 'ERROR', message: response.message, color:'red',position:'topRight', timeout: 6000 });
                $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>'+response.message+'</strong></div>');
              }
              $('#agentTransactionsDT').DataTable().ajax.reload();
           }
     });
  }