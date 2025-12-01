$(document).ready(function () {
    //send confirmation email
    $('#sendEmailThirdParty').click(function () {
        var post_arr = [];
        // sending confirmation email for transactions not in RUBIKON
        $('#notInCbsTxns input[type=checkbox]').each(function () {
            if (jQuery(this).is(":checked")) {
                var id = this.id;
                var datas = id.split('==');
                post_arr.push({
                    txnid: '<input hidden="" name="txnid" value="' + datas[0] + '">' + datas[0],
                    sourceaccount: '<input hidden="" name="sourceaccount" value="' + datas[1] + '">' + datas[1],
                    destinationaccount: '<input hidden="" name="destinationaccount" value="' + datas[2] + '">' + datas[2],
                    amount: '<input hidden="" name="amount" value="' + datas[3] + '">' + datas[3],
                    txndate: '<input hidden="" name="txndate" value="' + datas[4] + '">' + datas[4],
                    txn_status: '<input hidden="" name="txn_status" value="' + datas[5].substring(0, 10) + '">' + datas[5].substring(0, 10),
                });

            }
        });
        // sending confirmation email for transactions not in THIRD PARTY
        $('#notInThirdParty input[type=checkbox]').each(function () {
            if (jQuery(this).is(":checked")) {
                var id = this.id;
                var datas = id.split('==');
                post_arr.push({
                    txnid: '<input hidden="" name="txnid" value="' + datas[0] + '">' + datas[0],
                    sourceaccount: '<input hidden="" name="sourceaccount" value="' + datas[1] + '">' + datas[1],
                    destinationaccount: '<input hidden="" name="destinationaccount" value="' + datas[2] + '">' + datas[2],
                    amount: '<input hidden="" name="amount" value="' + datas[3] + '">' + datas[3],
                    txndate: '<input hidden="" name="txndate" value="' + datas[4] + '">' + datas[4],
                    txn_status: '<input hidden="" name="txn_status" value="' + datas[5].substring(0, 10) + '">' + datas[5].substring(0, 10),
                });

            }
        });
        if (post_arr.length > 0) {
//                    var isInitiated = confirm("Do you really want to Iniatiate?");
//                    if (isInitiated === true) {
            // AJAX Request
            $.ajax({
                url: "/initiateConfirmation",
                type: 'POST',
                success: function (response) {
                    $("#myModal").modal("show");
                    $("#areaValue").html(response);
                    $("#cbsTxnsRetryRefund").DataTable({
                        "data": post_arr,
                        lengthMenu: [
                            [-1],
                            ['Show all']
                        ],
                        "columns": [
                            {"data": "txndate"},
                            {"data": "txnid"},
                            {"data": "txnid"},
                            {"data": "sourceaccount"},
                            {"data": "destinationaccount"},
                            {"data": "amount"},
                        ],
                        responsive: !1,
                    });
                }
            });
//                    }
        } else {
            alert('No data selected');
        }
    });
    //approveRefundRetryTxnsModal
    $('#approveRefundRetryTxnsModal').click(function () {
        console.log('TRANSACTION LOGS.');
        var post_arr = [];
        var supportingDoc = "";
        var reason = "";
        var txn_type = $("select[name='mno']").val();
        // Get checked checkboxes
        $('#approveTxns input[type=checkbox]').each(function () {
            if (jQuery(this).is(":checked")) {

                var id = this.id;
                var datas = id.split('==');
                reason = datas[7];
                supportingDoc = datas[6];
                post_arr.push({
                    txnid: '<input hidden="" name="txnid" value="' + datas[0] + '">' + datas[0],
                    sourceAcct: '<input hidden="" name="sourceAcct" value="' + datas[1] + '">' + datas[1],
                    destinationAcct: '<input hidden="" name="destinationAcct" value="' + datas[2] + '">' + datas[2],
                    amount: '<input hidden="" name="amount" value="' + datas[3] + '">' + datas[3],
                    txndate: '<input hidden="" name="txndate" value="' + datas[4] + '">' + datas[4],
                    msisdn: '<input hidden="" name="msisdn" value="' + datas[8] + '">' + datas[8],
                    docode: '<input hidden="" name="docode" value="' + datas[9] + '">' + datas[9],
                    status: '<input hidden="" name="status" value="' + datas[5].substring(0, 10) + '">' + datas[5].substring(0, 10),
                });

            }
        });
        if (post_arr.length > 0) {
//                    var isInitiated = confirm("Do you really want to Iniatiate?");
//                    if (isInitiated === true) {
            // AJAX Request
            $.ajax({
                url: "/approveRefundRetryOnQueue",
                type: 'POST',
                data: {supportingDoc: supportingDoc, reason: reason, txn_type: txn_type},
                success: function (response) {
                    $("#myModal").modal("show");
                    $("#areaValue").html(response);
                    $("#cbsTxnsRetryRefund").DataTable({
                        "data": post_arr,
                        lengthMenu: [
                            [-1],
                            ['Show all']
                        ],
                        "columns": [
                            {"data": "txnid"},
                            {"data": "msisdn"},
                            {"data": "docode"},
                            {"data": "sourceAcct"},
                            {"data": "destinationAcct"},
                            {"data": "amount"},
                            {"data": "txndate"},
                            {"data": "status"}
                        ],
                        responsive: !1,
                    });
                }
            });
//                    }
        } else {
            alert('No data selected');
        }
    });
    //sync missing transactions
    $('#syncRubikonTxn').click(function () {
        $("#loading").show();
        console.log('TRANSACTION LOGS.');
        var post_arr = [];
        var txnid = "'0'";
        var sourceAcct = "'0'";
        var destinationAcct = "'0'";
        var txn_type;
        var supportingDoc = "";
        var reason = "";
        // Get checked checkboxes
        $('#notInCbsTxns input[type=checkbox]').each(function () {

            if (jQuery(this).is(":checked")) {

                var id = this.id;
                var datas = id.split('==');
                reason = datas[7];
                supportingDoc = datas[6];
                txnid += ",'" + datas[0] + "'";
                sourceAcct += ",'" + datas[1] + "'";
                destinationAcct += ",'" + datas[2] + "'";
                txn_type = datas[6];
                post_arr.push({
                    txnid: datas[0]
                });

            }
        });
        $('#searchTxns input[type=checkbox]').each(function () {

            if (jQuery(this).is(":checked")) {

                var id = this.id;
                var datas = id.split('==');
                reason = datas[7];
                supportingDoc = datas[6];
                txnid += ",'" + datas[0] + "'";
                sourceAcct += ",'" + datas[1] + "'";
                destinationAcct += ",'" + datas[2] + "'";
                txn_type = "B2C";
                post_arr.push({
                    txnid: datas[0]
                });

            }
        });
        if (post_arr.length > 0) {
            console.log(post_arr);
            var formdata = new FormData($("#syncCbstxns")[0]);
            console.log(formdata.toString());
            $.ajax({
                url: "/syncRubikonTxns",
                type: 'GET',
                data: {txnid: txnid, sourceAcct: sourceAcct, destinationAcct: destinationAcct, txn_type: txn_type},
                success: function (res) {
                    $("#loading").hide();
                    console.log(res);
                    $('#myModal').modal('hide');
                    $("#myModal").on('hidden.bs.modal', function () {
                        $(this).data('bs.modal', null);
                    });
                    $("#successFlag").show();
                    $("#successFlag").html('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + res.result + '</strong></div>');
                },
                error: function (res) {
                    $("#loading").hide();
                    console.log(res);
                    $('#myModal').modal('hide');
                    $("#successFlag").show();
                    $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + res.result + '</strong></div>');
                }
            });
//                    }
        } else {
            alert('No data selected');
        }
    });
    //confirm the transaction not in MNO/THIRDPARTY


    //sync missing transactions SEARCH

    //handle form select
    $('#tType').val("0");
    $('#txn_type').val("0");

    $('#tType').change(function () {
        ttypeVal = $('#tType').val();

        $('#txnType').change(function () {
            txnTypeVal = $('#txnType').val();
            if (ttypeVal == 0 && txnTypeVal == 0) {
                $('#syncRubikonTxnSearch').prop("disabled", true);
            } else {
                $('#syncRubikonTxnSearch').prop("disabled", false);
            }
        });
    });

    $('#syncRubikonTxnSearch').click(function () {
        $("#loading").show();
        var post_arr = [];
        var txnid = "'0'";
        var sourceAcct = "'0'";
        var destinationAcct = "'0'";
        var txn_type;
        var ttype;
        var supportingDoc = "";
        var reason = "";
        // Get checked checkboxes
        $('#notInCbsTxns input[type=checkbox]').each(function () {

            if (jQuery(this).is(":checked")) {

                var id = this.id;
                var datas = id.split('==');
                reason = datas[7];
                supportingDoc = datas[6];
                txnid += ",'" + datas[0] + "'";
                sourceAcct += ",'" + datas[1] + "'";
                destinationAcct += ",'" + datas[2] + "'";
                txn_type = datas[6];
                post_arr.push({
                    txnid: datas[0]
                });

            }
            console.log(post_arr);
        });
        $('#searchTxns input[type=checkbox]').each(function () {

            if (jQuery(this).is(":checked")) {

                var id = this.id;
                var datas = id.split('==');
                reason = datas[7];
                supportingDoc = datas[6];
                txnid += ",'" + datas[0] + "'";
                sourceAcct += ",'" + datas[1] + "'";
                destinationAcct += ",'" + datas[2] + "'";
//                        txn_type = "B2C";
                post_arr.push({
                    txnid: datas[0]
                });

            }
        });
        if (post_arr.length > 0) {
            console.log(post_arr);
            //var formdata = new FormData($("#syncRubikonTxnSearch")[0]);
            //console.log(formdata.toString());
            ttype = $('#tType').val();
            txn_type = $('#txnType').val();
            $.ajax({
                url: "/syncRubikonTxnSearch",
                type: 'GET',
                data: {txnid: txnid, sourceAcct: sourceAcct, destinationAcct: destinationAcct, txn_type: txn_type, ttype: ttype},
                success: function (res) {
                    $("#loading").hide();
                    console.log(res);
                    $('#myModal').modal('hide');
                    $("#myModal").on('hidden.bs.modal', function () {
                        $(this).data('bs.modal', null);
                    });
                    $("#successFlag").show();
                    $("#successFlag").html('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + res.result + '</strong></div>');
                },
                error: function (res) {
                    $("#loading").hide();
                    console.log(res);
                    $('#myModal').modal('hide');
                    $("#successFlag").show();
                    $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + res.result + '</strong></div>');
                }
            });
            //                    }
        } else {
            alert('No data selected');
        }
    });

    //END sync missing transactions SEARCH

    $('#confirmBulkCbs').click(function () {
        var post_arr = [];
        var txnids = "'0'";
        // confirm the transaction not in MNO/THIRDPARTY
        $('#notInThirdParty input[type=checkbox]').each(function () {
            if (jQuery(this).is(":checked")) {
                var id = this.id;
                var datas = id.split('==');
                txnids += ",'" + datas[0] + "'";
                post_arr.push({
                    txnid: datas[0],
                });

            }
        });
        // confirm the transaction not in CORE BANKING/CBS
        $('#notInCbsTxns input[type=checkbox]').each(function () {
            if (jQuery(this).is(":checked")) {
                var id = this.id;
                var datas = id.split('==');
                txnids += ",'" + datas[0] + "'";
                post_arr.push({
                    txnid: datas[0],
                });

            }
        });
        // confirm the transaction in core banking
        $('#cbsTransactions input[type=checkbox]').each(function () {
            if (jQuery(this).is(":checked")) {
                var id = this.id;
                var datas = id.split('==');
                txnids += ",'" + datas[0] + "'";
                post_arr.push({
                    txnid: datas[0],
                });

            }
        });
        // confirm the transaction in core banking success mno failed
        $('#cbsTxns input[type=checkbox]').each(function () {
            if (jQuery(this).is(":checked")) {
                var id = this.id;
                var datas = id.split('==');
                txnids += ",'" + datas[0] + "'";
                post_arr.push({
                    txnid: datas[0],
                });

            }
        });
        if (post_arr.length > 0) {
            console.log(post_arr);
            $.ajax({
                url: "/confirmBulkTxns",
                data: {txnid: txnids},
                type: 'GET',
                success: function (response) {
                    $("#myModal").modal("show");
                    $("#areaValue").html(response);
                }
            });
//                    }
        } else {
            alert('No data selected');
        }
    });
    //INITIATE REFUND RETRY OF TRANSACTIONS
    $('#initiateRefundRetryTxns').click(function () {
        var post_arr = [];

        var txn_type = $("select[name='mno']").val();

        console.log({txn_type:txn_type});
        // INITIATE RETRY TO CBS
        $('#notInCbsTxns input[type=checkbox]').each(function () {
            if (jQuery(this).is(":checked")) {
                var id = this.id;
                var datas = id.split('==');
//                console.log(datas);
                post_arr.push({
                    txnid: '<input hidden="" name="txnid" value="' + datas[0] + '">' + datas[0],
                    receiptNo: '<input hidden="" name="receiptNo" value="' + datas[7] + '">' + datas[7],
                    sourceAcct: '<input hidden="" name="sourceAcct" value="' + datas[1] + '">' + datas[1],
                    destinationAcct: '<input hidden="" name="destinationAcct" value="' + datas[2] + '">' + datas[2],
                    amount: '<input hidden="" name="amount" value="' + datas[3] + '">' + datas[3],
                    txndate: '<input hidden="" name="txndate" value="' + datas[4] + '">' + datas[4],
                    status: '<input hidden="" name="status" value="' + datas[5].substring(0, 10) + '">' + datas[5].substring(0, 10),
                });

            }
        });
        // INITIATE REFUND TO CBS
        $('#notInThirdParty input[type=checkbox]').each(function () {
            if (jQuery(this).is(":checked")) {
                var id = this.id;
                var datas = id.split('==');
//                console.log(datas);
                post_arr.push({
                    txnid: '<input hidden="" name="txnid" value="' + datas[0] + '">' + datas[0],
                    receiptNo: '<input hidden="" name="receiptNo" value="' + datas[6] + '">' + datas[6],
                    sourceAcct: '<input hidden="" name="sourceAcct" value="' + datas[1] + '">' + datas[1],
                    destinationAcct: '<input hidden="" name="destinationAcct" value="' + datas[2] + '">' + datas[2],
                    amount: '<input hidden="" name="amount" value="' + datas[3] + '">' + datas[3],
                    txndate: '<input hidden="" name="txndate" value="' + datas[4] + '">' + datas[4],
                    status: '<input hidden="" name="status" value="' + datas[5].substring(0, 10) + '">' + datas[5].substring(0, 10),
                });

            }
        });
        // INITIATE REFUND TO CBS
        $('#cbsTransactions input[type=checkbox]').each(function () {
            if (jQuery(this).is(":checked")) {
                var id = this.id;
                var datas = id.split('==');
//                console.log(datas);
                post_arr.push({
                    txnid: '<input hidden="" name="txnid" value="' + datas[0] + '">' + datas[0],
                    receiptNo: '<input hidden="" name="receiptNo" value="' + datas[6] + '">' + datas[6],
                    sourceAcct: '<input hidden="" name="sourceAcct" value="' + datas[1] + '">' + datas[1],
                    destinationAcct: '<input hidden="" name="destinationAcct" value="' + datas[2] + '">' + datas[2],
                    amount: '<input hidden="" name="amount" value="' + datas[3] + '">' + datas[3],
                    txndate: '<input hidden="" name="txndate" value="' + datas[4] + '">' + datas[4],
                    status: '<input hidden="" name="status" value="' + datas[5].substring(0, 10) + '">' + datas[5].substring(0, 10),
                });

            }
        });
        // INITIATE RETRY/REFUND FROM SEARCHED TRANSACTION FROM search transaction MENU
        $('#searchTxns input[type=checkbox]').each(function () {
            if (jQuery(this).is(":checked")) {
                var id = this.id;
                var datas = id.split('==');
//                console.log(datas);
                post_arr.push({
                    txnid: '<input hidden="" name="txnid" value="' + datas[0] + '">' + datas[0],
                    sourceAcct: '<input hidden="" name="sourceAcct" value="' + datas[1] + '">' + datas[1],
                    destinationAcct: '<input hidden="" name="destinationAcct" value="' + datas[2] + '">' + datas[2],
                    amount: '<input hidden="" name="amount" value="' + datas[3] + '">' + datas[3],
                    txndate: '<input hidden="" name="txndate" value="' + datas[4] + '">' + datas[4],
                    status: '<input hidden="" name="status" value="' + datas[5].substring(0, 10) + '">' + datas[5].substring(0, 10),
                });

            }
        });

        $('#notInLUKUSettlement input[type=checkbox]').each(function () {
            if (jQuery(this).is(":checked")) {
                var id = this.id;
                var datas = id.split('==');
//                console.log(datas);
                post_arr.push({
                    txnid: '<input hidden="" name="txnid" value="' + datas[0] + '">' + datas[0],
                    receiptNo: '<input hidden="" name="receiptNo" value="' + datas[7] + '">' + datas[0],
                    sourceAcct: '<input hidden="" name="sourceAcct" value="' + datas[1] + '">' + datas[1],
                    destinationAcct: '<input hidden="" name="destinationAcct" value="' + datas[6] + '">' + datas[6],
                    amount: '<input hidden="" name="amount" value="' + datas[3] + '">' + datas[3],
                    txndate: '<input hidden="" name="txndate" value="' + datas[4] + '">' + datas[4],
                    status: '<input hidden="" name="status" value="' + datas[5].substring(0, 10) + '">' + datas[5].substring(0, 10),
                });

            }
        })

        if (post_arr.length > 0) {
            $.ajax({
                url: "/initiateBulkRefundRetry",
                type: 'POST',
                data: {txn_type: txn_type},
                success: function (response) {
                    $("#myModal").modal("show");
                    $("#areaValue").html(response);
                    $("#cbsTxnsRetryRefund").DataTable({
                        "data": post_arr,
                        lengthMenu: [
                            [-1],
                            ['Show all']
                        ],
                        "columns": [
                            {"data": "txnid"},
                            {"data": "receiptNo"},
                            {"data": "sourceAcct"},
                            {"data": "destinationAcct"},
                            {"data": "amount"},
                            {"data": "txndate"},
                            {"data": "status"}
                        ],
                        responsive: !1,
                    });
                }
            });
//                    }
        } else {
            alert('No data selected');
        }
    });

    $('#LUKUInitiateRefundRetryTxns').click(function () {
        var post_arr = [];
        $('#notInLUKUSettlement input[type=checkbox]').each(function () {
            if (jQuery(this).is(":checked")) {
                var id = this.id;
                var datas = id.split('==');
                console.log(datas);
                post_arr.push({
                    txnid: '<input hidden="" name="txnid" value="' + datas[0] + '">' + datas[0],
                    sourceAcct: '<input hidden="" name="sourceAcct" value="' + datas[1] + '">' + datas[1],
                    destinationAcct: '<input hidden="" name="destinationAcct" value="' + datas[6] + '">' + datas[6],
                    amount: '<input hidden="" name="amount" value="' + datas[3] + '">' + datas[3],
                    txndate: '<input hidden="" name="txndate" value="' + datas[4] + '">' + datas[4],
                    status: '<input hidden="" name="status" value="' + datas[5].substring(0, 10) + '">' + datas[5].substring(0, 10),
                });

            }
        })

        if (post_arr.length > 0) {
        console.log(post_arr);
            $.ajax({
                url: "/LUKUInitiateBulkRefundRetry",
                type: 'POST',
                success: function (response) {
                    $("#myModal").modal("show");
                    $("#areaValue").html(response);
                    $("#cbsTxnsRetryRefund").DataTable({
                        "data": post_arr,
                        lengthMenu: [
                            [-1],
                            ['Show all']
                        ],
                        "columns": [
                            {"data": "txnid"},
                            {"data": "sourceAcct"},
                            {"data": "destinationAcct"},
                            {"data": "amount"},
                            {"data": "txndate"},
                            {"data": "status"}
                        ],
                        responsive: !1,
                    });
                }
            });
//                    }
        } else {
            alert('No data selected');
        }
    });

    $('#confrimation').click(function () {
        var post_arr = [];
        // Get checked checkboxes
        $('#cbsTxns input[type=checkbox]').each(function () {
            if (jQuery(this).is(":checked")) {
                var id = this.id;
                var datas = id.split('==');
                post_arr.push({
                    txnid: '<input hidden="" name="txnid" value="' + datas[0] + '">' + datas[0], amount: '<input hidden="" name="amount" value="' + datas[1] + '">' + datas[1], sourceaccount: '<input hidden="" name="sourceAccount" value="' + datas[2] + '">' + datas[2], destinationaccount: '<input hidden="" name="destinationAccount" value="' + datas[3] + '">' + datas[3]
                });

            }
        });
        if (post_arr.length > 0) {
//                    var isInitiated = confirm("Do you really want to Iniatiate?");
//                    if (isInitiated === true) {
            // AJAX Request
            $.ajax({
                url: "/sendConfirmationEmail",
                type: 'POST',
                success: function (response) {
                    $("#myModal").modal("show");
                    $("#areaValue").html(response);
                    $("#cbsTxnsRetryRefund").DataTable({
                        "data": post_arr,
                        "columns": [
                            {"data": "txnid"},
                            {"data": "sourceaccount"},
                            {"data": "destinationaccount"},
                            {"data": "amount"},
                        ],
                        responsive: !1,
                    });
                }
            });
//                    }
        } else {
            alert('No data selected');
        }
    });
    //set status of new opened mkoba accounts to solve ambiguous='Newly opened accounts on Rubikon'
    $('#syncMkobaNewOpenedAcct').click(function () {
        $("#loading").show();
        var post_arr = [];
        var txnids = "'0'";
        // Get checked checkboxes
        $('#notInThirdParty input[type=checkbox]').each(function () {
            if (jQuery(this).is(":checked")) {
                var id = this.id;
                var datas = id.split('==');
                txnids += ",'" + datas[0] + "'";
                post_arr.push({
                    txnid: '<input hidden="" name="txnid" value="' + datas[0] + '">' + datas[0], amount: '<input hidden="" name="amount" value="' + datas[1] + '">' + datas[1], sourceaccount: '<input hidden="" name="sourceAccount" value="' + datas[2] + '">' + datas[2], destinationaccount: '<input hidden="" name="destinationAccount" value="' + datas[3] + '">' + datas[3]
                });

            }
        });
        if (post_arr.length > 0) {
//                    var isInitiated = confirm("Do you really want to Iniatiate?");
//                    if (isInitiated === true) {
            // AJAX Request
            $.ajax({
                url: "/syncMkobaNewOpenedAcct",
                type: 'POST',
                data: {txnid: txnids},
                success: function (response) {
                    $("#loading").hide();
                    $("#successFlag").html('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>Your Request is being processed</strong></div>');
                }
            });
//                    }
        } else {
            alert('No data selected');
        }
    });
    //set status of new opened mkoba accounts to solve ambiguous='Newly opened accounts on Rubikon'
    $('#syncMkobaNoSignatories').click(function () {
        $("#loading").show();
        var post_arr = [];
        var txnids = "'0'";
        // Get checked checkboxes
        $('#notInCbsTxns input[type=checkbox]').each(function () {
            if (jQuery(this).is(":checked")) {
                var id = this.id;
                var datas = id.split('==');
                txnids += ",'" + datas[0] + "'";
                post_arr.push({
                    txnid: '<input hidden="" name="txnid" value="' + datas[0] + '">' + datas[0], amount: '<input hidden="" name="amount" value="' + datas[1] + '">' + datas[1], sourceaccount: '<input hidden="" name="sourceAccount" value="' + datas[2] + '">' + datas[2], destinationaccount: '<input hidden="" name="destinationAccount" value="' + datas[3] + '">' + datas[3]
                });

            }
        });
        if (post_arr.length > 0) {
//                    var isInitiated = confirm("Do you really want to Iniatiate?");
//                    if (isInitiated === true) {
            // AJAX Request
            $.ajax({
                url: "/syncMkobaNoSignatories",
                type: 'POST',
                data: {txnid: txnids},
                success: function (response) {
                    $("#loading").hide();
                    $("#successFlag").html('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>Your Request is being processed</strong></div>');
                }
            });
//                    }
        } else {
            alert('No data selected');
        }
    });
});
function confirmTxns(txnid) {
    $("#loading").show();
    $.ajax({
        url: "/confirmTransaction",
        data: {txnid: txnid},
        type: 'POST',
        success: function (response) {
            $("#loading").hide();
            $("#myModal").modal("show");
            $("#areaValue").html(response);
        }
    });
}

function checkForTokenTxns(txnid) {
    $("#loading").show();
    $.ajax({
        url: "/confirmTransaction",
        data: {txnid: txnid, txn_type: 'LUKUEX'},
        type: 'POST',
        success: function (response) {
            $("#loading").hide();
            $("#myModal").modal("show");
            $("#areaValue").html(response);
        }
    });
}

function checkForGepgTxns(txnid) {
    $("#loading").show();
    $.ajax({
        url: "/confirmTransaction",
        data: {txnid: txnid, txn_type: 'GEPGEX'},
        type: 'POST',
        success: function (response) {
            $("#loading").hide();
            $("#myModal").modal("show");
            $("#areaValue").html(response);
        }
    });
}
function ambiguousTxns(txnid) {
    $("#loading").show();
    $.ajax({
        url: "/initiateAmbiguousTransaction",
        data: {txnid: txnid},
        type: 'POST',
        success: function (response) {
            $("#loading").hide();
            //$("#cbsTxns").show();
            $("#myModal").modal("show");
            $("#areaValue").html(response);
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
function selectAllCore() {
    var items = document.getElementsByName('cbsTxnsDetails');
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
function UnSelectCoreSideAll() {
    var items = document.getElementsByName('cbsTxnsDetails');
    for (var i = 0; i < items.length; i++) {
        if (items[i].type === 'checkbox')
            items[i].checked = false;
    }
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
function reprocessTransaction(mno, ttype, txnid, sourceaccount, destinationaccount, amount, identifier) {
    var data2 = {mno: mno, ttype: ttype, txnid: txnid, sourceaccount: sourceaccount, destinationaccount: destinationaccount, amount: amount, identifier: identifier};
    event.preventDefault();
    $.ajax({
        type: 'GET',
        data: data2,
        url: "/initiateRetryRefund",
        success: function (data) {
            $("#myModal").modal("show");
            $("#areaValue").html(data);
        }
    });
}
function processBulkAmbiguous() {
    $("#ambigousBtn").hide();
    var post_arr = [];
    var hiddenData = "";
    var txndate;
    // Get checked checkboxes on thirdparty side
    $('#confirmThirdPartySide input[type=checkbox]').each(function () {
        if (jQuery(this).is(":checked")) {
            var id = this.id;
            var datas = id.split('==');
            hiddenData += '<input type="text"  name="identifier" value="existOnThirdParty" hidden=""/>';
            hiddenData += '<input type="text"  name="txnid" value="' + datas[0] + '" hidden=""/>' + '<input type="text"  name="thirdpartyTxndate" value="' + datas[1] + '" hidden=""/>';
            var datas = id;
            post_arr.push({
                txnid: datas,
            });
        }
    });
    // Get checked checkboxes on cbs side
    $('#confirmCBSSide input[type=checkbox]').each(function () {
        if (jQuery(this).is(":checked")) {
            var id = this.id;
            var datas = id.split('==');
            hiddenData += '<input type="text"  name="identifier" value="existOnCBS" hidden=""/>';
            hiddenData += '<input type="text"  name="txnid" value="' + datas[0] + '" hidden=""/>' + '<input type="text"  name="thirdpartyTxndate" value="' + datas[1] + '" hidden=""/>';
            var datas = id;
            post_arr.push({
                txnid: datas,
            });
        }
    });
    if (post_arr.length > 0) {

        document.querySelector('#content').insertAdjacentHTML(
                'afterbegin',
                `<div class="row">
                      <form role="form" name="bulkConfirmation" id="bulkConfirmation" enctype="multipart/form-data">
                       <div class="col-md-6">
                                <select class=" form-control" id="refundop" name="ambiguousOption" required>
                                <option value="off" selected>--SELECT AMBIGUOUS OPTION--</option>
                                <option value="1">MATCH THIRD PARTY Transaction DATE with CBS DATE</option>
                                <option value="2">MATCH CBS Transaction DATE with THIRD PARTY DATE</option>
                                <option value="3">SET THIRD PARTY Transaction as Success</option>
                                <option value="4">SET THIRD PARTY Transaction as reversed</option>
                                <option value="5">Cash Movements between Accounts</option>
                                <option value="6">Wrong destination Refunded by Third party</option>
                            </select>
                        </div>
           ` + hiddenData + `
                        <div class="col-md-4"><button type="button" class="btn btn-primary btn-bulkAmbiguous" onclick="bulkAmbiguousConfimation()">Submit</button></div>
                    </div>`
                )
    } else {
        $("#ambigousBtn").show();
        alert('No data selected');
    }

}
//bulk coambiguous submission
function bulkAmbiguousConfimation() {
    var ambiguousOption = $('#bulkConfirmation select[name=ambiguousOption]').val();
    if (ambiguousOption > 0) {
        var formdata = new FormData($("#bulkConfirmation")[0]);
        $.ajax({
            url: '/initiateBulkAmbiguousTxns',
            data: formdata,
            type: 'POST',
            enctype: 'multipart/form-data',
            processData: false,
            contentType: false,
            success: function (res) {
                console.log(res);
                $('#myModal').modal('hide');
                $("#myModal").on('hidden.bs.modal', function () {
                    $(this).data('bs.modal', null);
                });
                $("#successFlag").show();
                $("#successFlag").html('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>SUCCESS!!!!!!</strong></div>');
            },
            error: function (res) {
                console.log(res);
                $('#myModal').modal('hide');
                $("#successFlag").show();
                $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>Error!</strong></div>');
            }
        });
    } else {
        alert('Please select Ambiguous Option');
    }
}
//CONFIRM TRANSACTION ON RUBIKON
function confimTxnOnCBS(txnid) {
    $.ajax({
        url: '/searchCoreBanking',
        data: {txnid: txnid},
        type: 'POST',
        enctype: 'multipart/form-data',
        processData: false,
        contentType: false,
        success: function (res) {
            console.log(res);
            $('#myModal').modal('hide');
            $("#myModal").on('hidden.bs.modal', function () {
                $(this).data('bs.modal', null);
            });
            $("#successFlag").show();
            $("#successFlag").html(res);
        },
        error: function (res) {
            console.log(res);
            $('#myModal').modal('hide');
            $("#successFlag").show();
            $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>Error!</strong></div>');
        }
    });

}

//DOWNLOAD TRANSACTIONS FROM RUBIKON FOR RECON.
function downloadCBSDataForRecon() {
var mno = $("#mno").val();
var txnType = $("#txnType").val();
var txnDate = $("input[name='txnDate']").val();
 console.log("mno ==" + mno,"txnType ==" + txnType,"txnDate ==" + txnDate);
if(mno == ""){
 iziToast.error({ title: 'ERROR,', message: 'Please choose Txns Types', color:'red',position:'topRight', timeout: 3000 });
}else{
 iziToast.success({ title: 'PROCESSING,', message: 'System Is Processing.. Please wait. ', color:'green',position:'topRight', timeout: 30000 });
 $.ajax({
        url: '/fireDownloadCBSDataForRecon',
            data: {mno: mno, txnType: txnType, txnDate: txnDate},
        type: 'POST',
        enctype: 'multipart/form-data',
        processData: false,
        contentType: false,
        success: function (res) {
            console.log(res);
        },
        error: function (res) {
            console.log(res);
            $('#myModal').modal('hide');
            $("#successFlag").show();
            $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>Error in system processing!</strong></div>');
        }
    });
}

}
