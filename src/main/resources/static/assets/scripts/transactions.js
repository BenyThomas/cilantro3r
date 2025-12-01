//download supporting document for a transaction
$(document).ready(function () {
    //send confirmation email 
    $('#previewGePGBalances').click(function () {
        var accts = "'0'";
        var post_arr = [];
        $('#gegpgRemittanceBOT input[type=checkbox]').each(function () {
            if (jQuery(this).is(":checked")) {
                var id = this.id;
                var datas = id.split('==');
                accts += ",'" + datas[0] + "'";
                post_arr.push({
                    txnid: '<input hidden="" name="txnid" value="' + datas[0] + '">'
                });

            }
        });
        if (post_arr.length > 0) {
            console.log(accts);
            var token = $("meta[name='_csrf']").attr("content");
            var header = $("meta[name='_csrf_header']").attr("content");
            var table = $('#GepgRubikonAccountBalances').DataTable({
                dom: 'Blfrtip',
                "pageLength":1000,
                lengthChange: true,
                lengthMenu: [10, 25, 50, 100, 200, 300, 500, 1000, 2000, 5000, 10000, 25000],
                responsive: true,
                processing: true,
                serverSide: true,
                serverMethod: 'post',
                order: [1, "desc"],
                ajax: {
                    'url': '/previewGePGBalancesForRemittance',
                    'beforeSend': function (request) {
                        request.setRequestHeader(header, token);
                    },
                    "data": function (d) {
                        return $.extend({}, d, {
                            "accts": accts
                        });
                    }
                },
                "columns": [
                    {"data": 'SOURCEACCT'},
                    {"data": 'SOURCEACCT'},
                    {"data": "CURRENCY"},
                    {"data": "ACCOUNTNAME"},
                    {"data": 'AMOUNT',
                        "render": function (data) {
                            var amount = addCommas(data);
                            return amount;
                        }},
                    {"data": 'SOURCEACCT',
                        "orderable": false,
                        "searchable": false,
                        "render": function (data, type, row, meta) {
                            var a = '<button ><input type="checkbox" style="margin-top:10%" id="' + row.SOURCEACCT + '==' + row.ACCOUNTNAME + '==' + row.CURRENCY + '==' + row.AMOUNT + '" name="txndetails" /><input hidden="" name="txnid" value="' + row.SOURCEACCT + '"></button>';
                            return a;
                        }}
                ], aoColumnDefs: [
                    {
                        bSortable: false,
                        aTargets: [-1]
                    }
                ], buttons: [{
                        extend: "copy",
                        title: 'gepg account Transactions',
                        className: "btn-sm"
                    }, {
                        extend: "csv",
                        title: 'gepg account Transactions',
                        className: "btn-sm"
                    }, {
                        extend: "excel",
                        title: 'gepg account Transactions',
                        className: "btn-sm"
                    }, {
                        extend: "pdf",
                        title: 'gepg account Transactions',
                        className: "btn-sm"
                    }, {
                        extend: "print",
                        title: 'gepg account Transactions',
                        className: "btn-sm"
                    }],
                "preDrawCallback": function (settings) {
                    if ($("select[name='mno']").val() == '') {
                        return false;
                    }
                }
            });
            table.on('order.dt search.dt', function () {
                table.column(0, {search: 'applied', order: 'applied'}).nodes().each(function (cell, i) {
                    cell.innerHTML = i + 1;
                });
            }).draw();
            $('#gepgAccountsBalances').show();
            $('#gepgAccounts').hide();
            $('#selectTxnsType').hide();
        } else {
            alert('No data selected');
        }
    });
    //INITIATE GEPG REMITTANCE 
    $('#initiateGepgRemittance').click(function () {
        $('#initiatingGePGTxns').show();
        var accts = "";
        var post_arr = [];
        $('#GepgRubikonAccountBalances input[type=checkbox]').each(function () {
            if (jQuery(this).is(":checked")) {
                var id = this.id;
                var datas = id.split('==');
                accts += datas[0] + "==" + datas[1] + "==" + datas[2] + "==" + datas[3] + "__";
                post_arr.push({
                    sourceAcct: datas[0],
                    senderName: datas[1],
                    currency: datas[2],
                    amount: datas[3],
                });

            }
        });
        if (post_arr.length > 0) {
            $.ajax({
                url: "/initiateGePGRemittance",
                type: 'POST',
                data: {accts: accts,customNarration:$("#customNarration").val()},
                success: function (response) {
                    $('#initiatingGePGTxns').hide();
                    $("#initiateGepgRemittance").hide();
                    $("#gepgAccountsBalances").hide();
                    $("#successFlag").show();
                    $("#successFlag").html('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>\n\
                    '+response.result+'</strong></div>');
                    console.log(response);
                }
            });
        } else {
            alert('No data selected');
        }
    });
    $('#approvePayments').click(function () {
        $('#initiatingGePGTxns').show();
        var references = "0";
        var post_arr = [];
        console.log('here then');
        $('#approveGePGRemittance input[type=checkbox]').each(function () {
            if (jQuery(this).is(":checked")) {
                var id = this.id;
                var datas = id.split('==');
                references += "," + datas[0];
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
                        $("#successFlag").html('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + '</strong></div>');
                    } else {
                        $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + '</strong></div>');
                    }
                    console.log(response);
                }
            });
        } else {
            alert('No data selected');
        }
    });
    $('#approveGePGIBDPayments').click(function () {
        $('#loading').show();
        var references = "0";
        var txn_type;
        var post_arr = [];
        console.log('here then');
        $('#approveGePGPostingValRemittance input[type=checkbox]').each(function () {
            if (jQuery(this).is(":checked")) {
                var id = this.id;
                var datas = id.split('==');
                references += "," + datas[0];
                txn_type = datas[1];
                post_arr.push({
                    sourceAcct: datas[0]
                });

            }
        });
        if (post_arr.length > 0) {
            if (txn_type === '003') {
//                alert("its GePG Melleji!!!!!!!!!!!!!");
                $.ajax({
                    url: "/authorizeGePGPstngVldtinRemittance",
                    type: 'POST',
                    data: {references: references},
                    success: function (response) {
                        console.log(response);
                        $('#initiatingGePGTxns').hide();
                        $("#initiateGepgRemittance").hide();
                        $("#gepgAccountsBalances").hide();
                        $("#successFlag").show();
                        if (response.result == 0) {
                            $("#successFlag").html('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + '</strong></div>');
                        } else {
                            $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + '</strong></div>');
                        }
                        console.log(response);
                    }
                });
            } else {
                alert('This transaction type is not GePG TRANSACTION');
            }
        } else {
            alert('No data selected');
        }
    });
});
function downloadSupporting(reference) {
    $("#loading").show();
    $.ajax({
        url: "/downloadSupportingDoc",
        data: {reference: reference},
        type: 'POST',
        success: function (response) {
            $("#loading").hide();
            //$("#cbsTxns").show();
//            $("#myModal").modal("show");
//            $("#areaValue").html(response);
        }
    });
}

/*
 *
 * SM DPSS authorizes the cash movement
 */
function authorizeCMTransaction(reference) {
    $("#loading").show();
    $.ajax({
        url: "/firePreviewCashMovementTxn",
        data: {reference: reference},
        type: 'POST',
        success: function (response) {
            $("#loading").hide();
            $("#myModal").modal("show");
            $("#areaValue").html(response);
        }
    });
}

/*Authorize Mobile movement transaction to PV**/
function fireAuthMobileMovemntToPV(reference){
$('#fireAuthMobileMovemntToPVBtn').prop('disabled','disabled');
 $("#loading").show();
     $.ajax({
         url: "/fireAuthMobileMovemntToPV",
         data: {reference: reference},
         type: 'POST',
         success: function (response) {
                     $("#loading").hide();
                     $("#messageData").hide();
                     if (response.result == 0) {
                         $("#successFlag").after('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + '</strong></div>');
                         $('#mobile-movement-table').DataTable().ajax.reload();
                     } else {
                         $("#successFlag").after('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + response.result + '</strong></div>');
                     }
                     $('#successFlag').delay(2000).show();
                 }
     });
}


/*
 *
 * SM DPSS authorizes the cash movement
 */
function authorizeCMTransactionPVWF(reference) {

    $("#loading").show();
    $.ajax({
        url: "/firePreviewCashMovementTxnPVWF",
        data: {reference: reference},
        type: 'POST',
        success: function (response) {
            $("#loading").hide();
            $("#myModal").modal("show");
            $("#areaValue").html(response);
        }
    });
}

  /*Authorize Mobile movement transaction to PV**/
    function fireAuthMobileMovemntPVWFToGL(reference){
    $('#fireAuthMobileMovemntPVWFToGLBtn').prop('disabled','disabled');
     $("#loading").show();
         $.ajax({
             url: "/fireAuthMobileMovemntPVWFToGL",
             data: {reference: reference},
             type: 'POST',
             success: function (response) {
                         $("#loading").hide();
                         $("#messageData").hide();
                         if (response.result == 0) {
                             $("#successFlag").after('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + '</strong></div>');
                             $('#mobile-movement-table').DataTable().ajax.reload();
                         } else {
                             $("#successFlag").after('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + response.result + '</strong></div>');
                         }
                         $('#successFlag').delay(2000).show();
                     }
         });
    }
    function discardRemittanceTransaction(reference){
    $('#discardRemittanceTransaction').prop('disabled','disabled');
     $("#loading").show();
         $.ajax({
             url: "/fireDiscardRemittanceTransaction",
             data: {reference: reference},
             type: 'POST',
             success: function (response) {
                         $("#loading").hide();
                         $("#messageData").hide();
                         if (response.result == 0) {
                             $("#successFlag").html('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + '</strong></div>');
                             $('#approveGePGRemittance').DataTable().ajax.reload();
                         } else {
                             $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + response.result + '</strong></div>');
                         }
                         $('#successFlag').delay(2000).show();
                     }
         });
    }

    /*
     *
     * SM DPSS authorizes the cash movement
     */
    function authorizeVikobaTransaction(reference) {
        $("#loading").show();
        $.ajax({
            url: "/firePreviewVikobaFundMovementTxn",
            data: {reference: reference},
            type: 'POST',
            success: function (response) {
                $("#loading").hide();
                $("#myModal").modal("show");
                $("#areaValue").html(response);
            }
        });
    }

    /*Authorize vikoba movement transaction**/
    function fireAuthVikobaFundMovemnt(reference){
    $('#fireAuthMobileMovemntToPVBtn').prop('disabled','disabled');
     $("#loading").show();
         $.ajax({
             url: "/fireAuthVikobaFundMovemnt",
             data: {reference: reference},
             type: 'POST',
             success: function (response) {
                         $("#loading").hide();
                         $("#messageData").hide();
                         if (response.result == 0) {
                             $("#successFlag").after('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + '</strong></div>');
                             $('#mobile-movement-table').DataTable().ajax.reload();
                         } else {
                             $("#successFlag").after('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + response.result + '</strong></div>');
                         }
                         $('#successFlag').delay(2000).show();
                     }
         });
    }


    function previewStaffSalary(batchReference,action) {
        $("#loading").show();
        $("#previewCurrentMonthlySalary").attr('disabled','disabled');
        $.ajax({
            url: action,
            data: {batchReference: batchReference},
            type: 'POST',
            success: function (response) {
              $("#loading").hide();
              $("#myModal").modal("show");
              $("#areaValue").html(response);
            }
        });
    }


function fireAuthCurrentMonthSalary(batchReference){
    $('#fireAuthCurrentMonthSalaryBtn').prop('disabled','disabled');
             $.ajax({
                 url: "/fireAuthCurrentMonthSalary",
                 data: {batchReference: batchReference },
                 type: 'POST',
                 success: function (response) {
                             $("#loading").hide();
                             $("#messageData").hide();

                             if (response.result == 0) {
                                         iziToast.success({ title: 'SUCCESS', message: 'SALARY IS PROCESSING, PLEASE WAIT', color:'green',position:'topRight', timeout: 7000 });
                                 $("#successFlag").after('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + '</strong></div>');
                                 $('#staff-salary-table').DataTable().ajax.reload();
                             } else {
                                 $("#successFlag").after('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + response.result + '</strong></div>');
                             }
                             $('#successFlag').delay(2000).show();
                         }
             });
}


function authorizeBackOfficeManuallRepayment(reference) {

    $("#loading").show();
    $.ajax({
        url: "/fireApprovalByBackOfficeLoanRepayment",
        data: {reference: reference},
        type: 'POST',
        success: function (response) {
            $("#loading").hide();
            $("#myModal").modal("show");
            $("#areaValue").html(response);
        }
    });
}

/*Authorize Manual Loan Repayment**/
    function fireAuthManualLoanRepayment(reference){
    $('#authLoanBtn').prop('disabled','disabled');
     $("#loading").show();
//        iziToast.success({ title: 'PROCESSING', message: 'Processing Manual Loan Repayment', color:'green',position:'topRight', timeout: 5000 });
         $.ajax({
             url: "/fireAuthManualLoanRepaymentAjax",
             data: {reference: reference},
             type: 'POST',
             success: function (response) {
                         $("#loading").hide();
                         $("#messageData").hide();
                         if (response.result == 0) {
                             $("#successFlag").after('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + '</strong></div>');
                             $('#dtableVerification').DataTable().ajax.reload();
                         } else {
                             $("#successFlag").after('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message +" with code: " + response.result + '</strong></div>');
                         }
                         $('#successFlag').delay(2000).show();
                     }
         });
    }