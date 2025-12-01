/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

let arr = [];
let batchesMatchTiss = false;
let tissRef;
let tissAmount;

function previewPensionPayrollDetails(batchReference, status) {
    $("#loading").show();
    $.ajax({
        url: "/viewPensionPayrollDetails",
        data: {batchReference: batchReference, status: status},
        type: 'POST',
        success: function (response) {
            $("#loading").hide();
            $("#myModal").modal("show");
            $("#areaValue").html(response);
        }
    });
}

function processPensionTransactionsConfirmation() {
    $("#ambigousBtn").hide();
    $("#actionProcessed").hide();
    var data = $('#pensionBatchSummary').DataTable().rows({ selected: true }).data();
    if (data.length > 0) {
        for (var i = 0; i < data.length; i++) {
            const obj = {};
            obj.reference = data[i].reference;
            obj.successCount = data[i].successCount;
            obj.successTxnVolume = data[i].successTxnVolume;
            obj.failedCount = data[i].failedCount;
            obj.failedTxnVolume = data[i].failedTxnVolume;
            obj.noOfTxns = data[i].noOfTxns;
            obj.narration = data[i].batchDescription;
            arr.push(obj);
        }
    } else {
        $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert" aria-label="Close">x</button><strong>No batch selected!</strong></div>').show();
        return;
    }
    swal({
        title: "Are you sure?",
        text: "This is irreversible, click process button if you are sure.",
        type: "warning",
        showCancelButton: true,
        confirmButtonColor: "#3ba03e",
        confirmButtonText: "Process Pension"
    })
    .then((value) => {
        if (value)
            processPensionToCoreBanking()
    });
}

function processPensionToCoreBanking() {
    var data;
    if (typeof arr === "string")
        data = {batchReference: arr};
    else if (Array.isArray(arr))
        data = {arr: arr, isArray: true};
    showEnterReferenceDialog(data);
}

function processToCbs() {
//    if (batchesMatchTiss) {
        $("#loading").show();
        $.ajax({
            url: "/processPensionPayrollToCoreBanking",
            type: 'POST',
            data: {data: JSON.stringify(arr), tissRef: tissRef, tissAmount: tissAmount},
            dataType: 'JSON',
            success: function (response) {
                $("#loading").hide();
                if (response.result == 0) {
                    $("#successFlag").html('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert" aria-label="Close">x</button><strong>' + response.message + '</strong></div>').show();
                    $('#pensionBatchSummary').DataTable().ajax.reload();
                } else {
                    if (response.message !== undefined && response.result !== undefined)
                        $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert" aria-label="Close">x</button><strong>' + response.message + ' (' + response.result + ')</strong></div>').show();
                    else
                        $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert" aria-label="Close">x</button><strong>General error!</strong></div>').show();
                }
            },
            error: function (response) {
                $("#loading").hide();
                $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert" aria-label="Close">x</button><strong>' + response.message + ' (' + response.result + ')</strong></div>').show();
            }
        });
//    } else {
//        $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert" aria-label="Close">x</button><strong>The total batch amount do not match with TISS amount... cannot process to CBS!</strong></div>').show();
//    }
}

function complyTransactionWithReason(batchReference, status) {
    var data = $('#inwardEFTBankSuccessSummary').DataTable().rows({ selected: true }).data();
    var arr = [];
    if (data.length > 0) {
        for (var i = 0; i < data.length; i++) {
            arr.push(data[i].trackingNo);
        }
    } else {
        $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert" aria-label="Close">x</button><strong>No transaction selected!</strong></div>').show();
        return;
    }
    $.ajax({
        url: "/complyTransactionWithReason",
        type: 'POST',
        data: {batchReference: batchReference, status: status, trackingNos: JSON.stringify(arr), all: all},
        success: function (response) {
            if (response.result == 0) {
                $("#successFlag").html('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert" aria-label="Close">x</button><strong>' + response.message + '</strong></div>').show();
                $('#pensionBatchSummary').DataTable().ajax.reload();
            } else {
                $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert" aria-label="Close">x</button><strong>' + response.message + ' (' + response.result + ')</strong></div>').show();
            }
        },
        error: function (response) {
            $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert" aria-label="Close">x</button><strong>' + response.message + ' (' + response.result + ')</strong></div>').show();
        }
    });
}

function approveCompliance() {
    var data = $('#pensionBatchSummary').DataTable().rows({ selected: true }).data();
    var arr = [];
    if (data.length > 0) {
        for (var i = 0; i < data.length; i++) {
            arr.push(data[i].reference);
        }
    } else {
        $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert" aria-label="Close">x</button><strong>No batch selected!</strong></div>').show();
        return;
    }
    $.ajax({
        url: "/approveCompliance",
        type: 'POST',
        data: {batchReferences: JSON.stringify(arr)},
        success: function (response) {
            if (response.result == 0) {
                $("#successFlag").html('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert" aria-label="Close">x</button><strong>' + response.message + '</strong></div>').show();
                $('#pensionBatchSummary').DataTable().ajax.reload();
            } else {
                $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert" aria-label="Close">x</button><strong>' + response.message + ' (' + response.result + ')</strong></div>').show();
            }
        },
        error: function (response) {
            $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert" aria-label="Close">x</button><strong>' + response.message + ' (' + response.result + ')</strong></div>').show();
        }
    });
}

function returnTransactionWithReason(batchReference) {
    var data = $('#pensionBatchTransactions').DataTable().rows({ selected: true }).data();
    if (data.length > 0) {
        for (var i = 0; i < data.length; i++) {
            arr.push(data[i].bankReference);
        }
    } else {
        $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert" aria-label="Close">x</button><strong>No batch selected!</strong></div>').show();
        return;
    }
    $("#loading").show();
    $.ajax({
        url: "/returnTransactionWithReason",
        type: 'POST',
        data: {batchReference: batchReference, bankReferences: JSON.stringify(arr)},
        success: function (response) {
            $("#loading").hide();
            if (response.result == "200") {
                $("#successFlag").html('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert" aria-label="Close">x</button><strong>' + response.message + '</strong></div>').show();
            } else {
                $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert" aria-label="Close">x</button><strong>' + response.message + ' (' + response.result + ')</strong></div>').show();
            }
        },
        error: function (response) {
            $("#loading").hide();
            $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert" aria-label="Close">x</button><strong>' + response.message + ' (' + response.result + ')</strong></div>').show();
        }
    });
}

function authorizeTransactionsWithReason() {
    var data = $('#pensionBatchSummary').DataTable().rows({ selected: true }).data();
    var arr = [];
    if (data.length > 0) {
        for (var i = 0; i < data.length; i++) {
            arr.push(data[i].reference);
        }
    }
    $.ajax({
        url: "/authorizeTransactionsWithReason",
        type: 'POST',
        data: {batchReferences: JSON.stringify(arr)},
        success: function (response) {
            if (response.result == 0) {
                $("#successFlag").html('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert" aria-label="Close">x</button><strong>' + response.message + '</strong></div>').show();
                $('#pensionBatchSummary').DataTable().ajax.reload();
            } else {
                $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert" aria-label="Close">x</button><strong>' + response.message + ' (' + response.result + ')</strong></div>').show();
            }
        },
        error: function (response) {
            $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert" aria-label="Close">x</button><strong>' + response.message + ' (' + response.result + ')</strong></div>').show();
        }
    });
}

function approveAuthorizedTransactions() {
    var data = $('#pensionBatchSummary').DataTable().rows({ selected: true }).data();
    var arr = [];
    if (data.length > 0) {
        for (var i = 0; i < data.length; i++) {
            arr.push(data[i].reference);
        }
    }
    $.ajax({
        url: "/approveAuthorizedTransactions",
        type: 'POST',
        data: {batchReferences: JSON.stringify(arr)},
        success: function (response) {
            if (response.result == 0) {
                $("#successFlag").html('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert" aria-label="Close">x</button><strong>' + response.message + '</strong></div>').show();
                $('#pensionBatchSummary').DataTable().ajax.reload();
            } else {
                $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert" aria-label="Close">x</button><strong>' + response.message + ' (' + response.result + ')</strong></div>').show();
            }
        },
        error: function (response) {
            $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert" aria-label="Close">x</button><strong>' + response.message + ' (' + response.result + ')</strong></div>').show();
        }
    });
}

function showEnterReferenceDialog(data) {
    swal({
        title: 'TISS Reference Search Form',
        html: `<input type="text" id="reference" name="reference" class="swal2-input" placeholder="Enter reference" autocomplete="off" th:required="true">`,
        type: "question",
//        allowOutsideClick: false,
        confirmButtonText: 'Search',
        confirmButtonColor: "#3ba03e"
    })
    .then((result) => {
        if (result) {
            tissRef = $('#reference').val();
            if (!reference) {
                swal.showValidationMessage(`Please enter TISS reference!`)
            } else {
                var successTxnVolume = arr.reduce(function (a, b) {
                    return a + b.successTxnVolume;
                }, 0);
                console.log(successTxnVolume);
                var failedTxnVolume = arr.reduce(function (a, b) {
                    return a + b.failedTxnVolume;
                }, 0);
                console.log(failedTxnVolume);
                var successCount = arr.reduce(function (a, b) {
                    return a + b.successCount;
                }, 0);
                console.log(successCount);
                var failedCount = arr.reduce(function (a, b) {
                    return a + b.failedCount;
                }, 0);
                console.log(failedCount);
                var noOfTxns = arr.reduce(function (a, b) {
                    return a + b.noOfTxns;
                }, 0);
                var successTxnVolume = arr.reduce(function (a, b) {
                    return a + b.successTxnVolume;
                }, 0);
                console.log(successTxnVolume);
                var failedTxnVolume = arr.reduce(function (a, b) {
                    return a + b.failedTxnVolume;
                }, 0);
                console.log(failedTxnVolume);
                var successCount = arr.reduce(function (a, b) {
                    return a + b.successCount;
                }, 0);
                console.log(successCount);
                var failedCount = arr.reduce(function (a, b) {
                    return a + b.failedCount;
                }, 0);
                console.log(failedCount);
                var noOfTxns = arr.reduce(function (a, b) {
                    return a + b.noOfTxns;
                }, 0);

                console.log(failedCount);
                var noOfTxns = arr.reduce(function (a, b) {
                    return a.noOfTxns + b.noOfTxns;
                });
                console.log(noOfTxns);
                $.ajax({
                    url: "/getAmountFromReference",
                    type: 'POST',
                    data: {reference: tissRef},
                    success: function (response) {
                        if (response.result == 0) {
                            tissAmount = response.amount;
                            var totalBatch = (successTxnVolume + failedTxnVolume).toFixed(2);
                            if (Math.abs(tissAmount - totalBatch) < 100) {
                                batchesMatchTiss = true;
                            } else {
                                batchesMatchTiss = false;
                            }
                            swal({
                                title: 'TISS Reference: '+ tissRef,
                                html: `<h4 class="text-success">Total success amount is: Tshs. ${addCommas(`${successTxnVolume}`)}</h4>
                                    <h4 class="text-danger">Total failure amount is: Tshs. ${addCommas(`${failedTxnVolume}`)}</h4>
                                    <h4 class="text-primary">Total amount for batches is: Tshs. ${addCommas(`${totalBatch}`)}</h4>
                                    <h4>Total TISS amount is: Tshs. ${addCommas(`${tissAmount}`)}</h4>`,
                                type: "success",
//                                allowOutsideClick: false,
                                confirmButtonText: 'Process To CBS',
                                confirmButtonColor: "#3ba03e",
                            }).then((isConfirm) => {
                                if (isConfirm)
                                    processToCbs();
                            });
                        } else {
                            swal("Error!", "Reference doesn\'t exist!", "error");
                        }
                    },
                    error: function (response) {
                        swal("Error!", response.message + " (" + response.result + ")", "error");
                    }
                });
            }
        }
    });
}

function processBatch(batchReference) {
    $("#loading").show();
    $.ajax({
        url: "/processTempBatches",
        type: 'POST',
        data: {batchReference: batchReference},
        success: function (response) {
            $("#loading").hide();
            if (response.result == "0") {
                $("#successFlag").html('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert" aria-label="Close">x</button><strong>' + response.message + '</strong></div>').show();
                $('#pensionBatchSummary').DataTable().ajax.reload();
            } else {
                $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert" aria-label="Close">x</button><strong>' + response.message + ' (' + response.result + ')</strong></div>').show();
            }
        },
        error: function (response) {
            $("#loading").hide();
            $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert" aria-label="Close">x</button><strong>' + response.message + ' (' + response.responseCode + ')</strong></div>').show();
        }
    });
}

function viewTempTransactions(batchReference) {
    $("#loading").show();
    var htmlData = '<div class="row"><div class="panel panel-default"><div class="panel-body"><div id="modal-alert"></div>';
    htmlData += '<table id="batchTransactions" class="display nowrap table table-striped table-bordered" style="width: 100%; font-size: 9.5px !important;">';
    htmlData += '<thead>';
    htmlData += '<tr>';
    htmlData += '<th>S/N</th>';
    htmlData += '<th>Rec ID</th>';
    htmlData += '<th>Txn Ref</th>';
    htmlData += '<th>Batch</th>';
    htmlData += '<th>Currency</th>';
    htmlData += '<th>Amount</th>';
    htmlData += '<th>Credit Account</th>';
    htmlData += '<th>Debit Account</th>';
    htmlData += '<th>Narration</th>';
    htmlData += '<th>Status</th>';
    htmlData += '<th>Result</th>';
    htmlData += '<th>Tries</th>';
    htmlData += '<th>Created At</th>';
    htmlData += '<th>Updated At</th>';
    htmlData += '</tr>';
    htmlData += '</thead>';
    htmlData += '<tbody></tbody>';
    htmlData += '</table>';
    htmlData += '</div></div></div>';
    $("#loading").hide();
    $("#myModal").modal("show");
    $("#areaValue").html(htmlData);
    var table = $('#batchTransactions').DataTable({
        dom: 'Blfrtip',
        lengthChange: true,
        scrollCollapse: true,
        scrollY: "700px",
        scrollX: true,
        pageLength: 10,
        lengthMenu: [
            [5, 10, 25, 50, 100, 500, -1],
            [5, 10, 25, 50, 100, 500, 'Show all']
        ],
        responsive: false,
        processing: true,
        serverSide: true,
        serverMethod: 'POST',
        order: [1, "asc"],
        ajax: {
            'url': '/previewTempBatchTransactions',
            'data': function (d) {
                return $.extend({}, d, {
                    "reference": batchReference
                });
            }
        },
        "columns": [
            {"data": null},
            {"data": "recId"},
            {"data": "txnRef"},
            {"data": "batch"},
            {"data": "currency"},
            {"data": "amount"},
            {"data": "crAcct"},
            {"data": "drAcct"},
            {"data": "narration"},
            {"data": "recSt"},
            {"data": "result"},
            {"data": "tries"},
            {"data": "createDt"},
            {"data": "updateDt"}
        ], aoColumnDefs: [
            {
                bSortable: false,
                aTargets: [-1],
            }
        ], columnDefs: [
            {width: 1000, targets: 14}
        ],
        buttons: [{
                extend: "copy",
                title: 'PENSION BATCH TRANSACTIONS',
                className: "btn-sm"
            }, {
                extend: "csv",
                title: 'PENSION BATCH TRANSACTIONS',
                className: "btn-sm"
            }, {
                extend: "excel",
                title: 'PENSION BATCH TRANSACTIONS',
                className: "btn-sm"
            }, {
                extend: "pdf",
                title: 'PENSION BATCH TRANSACTIONS',
                className: "btn-sm"
            }, {
                extend: "print",
                title: 'PENSION BATCH TRANSACTIONS',
                className: "btn-sm"
            }
        ]
    });
    table.on('order.dt search.dt', function () {
        table.column(0, {search: 'applied', order: 'applied'}).nodes().each(function (cell, i) {
            cell.innerHTML = i + 1;
        });
    }).draw();
}