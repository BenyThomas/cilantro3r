function previewBatchTransactions(batchReference, accountNo, totalAmount, status) {
    $("#loading").show();
    if (status === 'undefined') status = 'R';
    var userRoles = $("input[name=userRoles]").val();
    var htmlData = '<div class="panel panel-default"><div class="panel-body">';
    htmlData += '<h2 id="batch-ref" class="text-center"></h2>';
    htmlData += '<div class="row"><div class="col-md-6">';
    htmlData += '<table id="batchTransactions" class="display nowrap table table-striped table-bordered" style="width: 100%; font-size: 9.5px !important;">';
    htmlData += '<thead>';
    htmlData += '<tr><th colspan="15" id="bt-title"></th></tr>';
    htmlData += '<tr>';
    htmlData += '<th>S/N</th>';
    htmlData += '<th>Priority</th>';
    htmlData += '<th>Vendor No</th>';
    htmlData += '<th>End To End ID</th>';
    htmlData += '<th>Transaction Amount</th>';
    htmlData += '<th>Beneficiary Name</th>';
    htmlData += '<th>Beneficiary BIC</th>';
    htmlData += '<th>Description</th>';
    htmlData += '<th>DISB Number</th>';
    htmlData += '<th>Unapplied Account</th>';
    htmlData += '<th>Batch Reference</th>';
    htmlData += '<th>status</th>';
    htmlData += '<th>Payment Channel</th>';
    htmlData += '<th>Bank Reference</th>';
    htmlData += '<th>Transaction Date</th>';
    htmlData += '</tr>';
    htmlData += '</thead>';
    htmlData += '<tbody></tbody>';
    htmlData += '</table>';
    htmlData += '<div class="form-group"><button type="button" class="btn btn-primary" ';
    htmlData += 'onclick="approveBatchTransactions(\'' + batchReference + '\',\'' + accountNo + '\',\'' + status + '\')">';
    if ((userRoles.includes("27") || userRoles.includes("21")) && status == "I") {
        htmlData += 'Approve';
    } else if ((userRoles.includes("31") || userRoles.includes("21")) && status == "A") {
        htmlData += 'Authorize';
//    } else if ((userRoles.includes("24") || userRoles.includes("21")) && status == "P") {
//        htmlData += 'Check</button>';
    } else if ((userRoles.includes("24") || userRoles.includes("21")) && status == "C") {
        htmlData += 'Settle';
    }
    htmlData += '</button></div>';
    htmlData += '<div class="form-group">';
    htmlData += '<label for="textarea">Server output</label>';
    htmlData += '<textarea class="form-control" id="output" rows="10" readonly></textarea>';
    htmlData += '</div>';
    htmlData += '</div>';
    htmlData += '<div class="col-md-6">';
    htmlData += '<h3 id="file-name" class="text-center"></h3>';
    htmlData += '<iframe id="embedPreview" width="100%" height="480" frameborder="1" scrolling="yes"/>';
    htmlData += '</div></div></div></div>';
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
        searching: false,
        paging: false,
        serverMethod: 'POST',
        order: [1, "asc"],
        ajax: {
            'url': '/previewBatchTransactionsList',
            'data': function (d) {
                return $.extend({}, d, {
                    "reference": batchReference,
                    "account": accountNo
                });
            }
        },
        "columns": [
            {"data": null},
            {"data": "priority"},
            {"data": "vendorNo"},
            {"data": "endToEndId"},
            {"data": "trxAmount"},
            {"data": "beneficiaryName"},
            {"data": "beneficiaryBic"},
            {"data": "disbNumber"},
            {"data": "unappliedAccount"},
            {"data": "detailsOfCharge"},
            {"data": "batchReference"},
            {"data": "status"},
            {"data": "paymentChannel"},
            {"data": "bankReference"},
            {"data": "txnDate"}
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
                title: 'MUSE/ERMS PENDING BATCH TRANSACTIONS',
                className: "btn-sm"
            }, {
                extend: "csv",
                title: 'MUSE/ERMS PENDING BATCH TRANSACTIONS',
                className: "btn-sm"
            }, {
                extend: "excel",
                title: 'MUSE/ERMS PENDING BATCH TRANSACTIONS',
                className: "btn-sm"
            }, {
                extend: "pdf",
                title: 'MUSE/ERMS PENDING BATCH TRANSACTIONS',
                className: "btn-sm"
            }, {
                extend: "print",
                title: 'MUSE/ERMS PENDING BATCH TRANSACTIONS',
                className: "btn-sm"
            }
        ],
        "initComplete": function(settings, json) {
            $("#bt-title").html("Batch " + batchReference + ", Count: " + json.aaData.length + ", Total: "
             + totalAmount.toLocaleString('sw-TZ'));
        }
    });
    table.on('order.dt search.dt', function () {
        table.column(0, {search: 'applied', order: 'applied'}).nodes().each(function (cell, i) {
            cell.innerHTML = i + 1;
        });
    }).draw();

    $.ajax({
        url: "/getBatchDoc",
        data: {reference: batchReference},
        type: 'POST',
        success: function (response) {
            $("#loading").hide();
            var isIE = false || !!document.documentMode;
            if (isIE) {
                var bytes = _base64ToArrayBuffer(response.fileContent);
                saveByteArray(response.fileName, bytes);
            } else {
                $("#embedPreview").attr('src', 'data:text/plain;base64,' + response.fileContent);
            }
            $("#batch-ref").html(response.reference);
            $("#file-name").html(response.fileName);
        }
    });
}

function approveBatchTransactions(ref, accountNo, status) {
    var stompClient = null;
    var eventCode;
    if (status == "I") { // approve
        eventCode = "IN_0011";
    } else if (status == "A") { // authorize
        eventCode = "IN_002";
    } else if (status == "P") { // check
        eventCode = "IN_0022";
    } else if (status == "C") { // settle
        eventCode = "IN_003";
    }
    $("#loading").show();
    $.ajax({
        url: "/processBatch",
        data: {reference: ref, eventCode: eventCode, status: status},
        type: 'POST',
        success: function (res) {
            $("#loading").hide();
            if(res.result == -1) {
                $("#myModal").modal('toggle');
                $('#successFlag').html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert" aria-label="Close"></button><strong>' + res.status + ', ' + res.message + '</strong></div>');
                setTimeout(function() {
                    $("#role_button").click();
                }, 1000);
                setTimeout(function() {
                    $('#successFlag').html('');
                }, 1500);
            } else {
                var socket = new SockJS('/batchProcessingAsyncUrl');
                stompClient = Stomp.over(socket);
                stompClient.connect({}, function (frame) {
                    console.log('Connected: ' + frame);
                    stompClient.subscribe('/topic/batchProcessingAsyncUrl', function (output) {
//                        console.log(JSON.parse(output.body));
                        $("#output").val(function(index, val) {
                            return val + "\n" + output.body;
                        });
                    });
                });
                var startTime = new Date().getTime();
                var interval = setInterval(function() {
                    let time = new Date().toLocaleTimeString();
                    let json = {"from": "server", "time": time};
                    stompClient.send("/esb/batchProcessingAsyncUrl", {}, JSON.stringify(json));
                }, 5000);
                setTimeout(function() {
                    clearInterval(interval);
                }, 120000);
                if (res.responseCode == '0')
                    $('#successFlag').html('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert" aria-label="Close"></button><strong>' + res.reference + ': ' + res.message + '</strong></div>');
                else
                    $('#successFlag').html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert" aria-label="Close"></button><strong>' + res.reference + ': ' + res.message + '</strong></div>');
            }
        },
        error: function (res) {
            console.log(res);
            $("#loading").hide();
            $('#successFlag').html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert" aria-label="Close"></button><strong>' + res.status + ': ' + res.message + '</strong></div>');
        }
    });
}

function _base64ToArrayBuffer(base64) {
        var binary_string = window.atob(base64);
        var len = binary_string.length;
        var bytes = new Uint8Array(len);
        for (var i = 0; i < len; i++) {
            bytes[i] = binary_string.charCodeAt(i);
        }
        return bytes.buffer;
    }

function saveByteArray(reportName, byte) {
        var blob = new Blob([byte], { type: "text/plain" });
        var isIE = false || !!document.documentMode;
        if (isIE) {
            window.navigator.msSaveBlob(blob, reportName);
        } else {
            var url = window.URL || window.webkitURL;
            link = url.createObjectURL(blob);
            var a = $("<a />");
            a.attr("download", reportName);
            a.attr("href", link);
            $("body").append(a);
            a[0].click();
            $("body").remove(a);
        }
    }