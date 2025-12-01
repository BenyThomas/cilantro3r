function previewBatchTransactions(batchReference, accountNo, status, userRoles) {
    $("#loading").show();
    if (status === 'undefined') status = 'R';
    var htmlData = '<div class="row"><div class="panel panel-default"><div class="panel-body"><div id="modal-alert"></div>';
    if ((userRoles.includes("26") || userRoles.includes("21")) && status == "R") {
        htmlData += '<div style="display:inline-block; margin: auto"><input type="file" id="transactionFile" class="form-control form-control-sm"'+
            'style="font-size: 10px !important;" placeholder="Select Document">'+
            '<button type="button" class="btn btn-primary" onclick="initiateBatch(\'' + batchReference + '\', \'IN_001\')">Upload</button></div>';
    }
    if (status == "I" || status == "C") {
        htmlData += '<div style="display:inline-block; margin:10px 0"><button type="button" class="btn btn-primary"' +
         'onclick="previewSupportingDoc(\'' + batchReference +'\',\'' + accountNo +'\',\'' + status +'\',\'' + userRoles +'\')">Preview Document</button></div>';
    }
    htmlData += '<table id="batchTransactions" class="display nowrap table table-striped table-bordered" style="width: 100%; font-size: 9.5px !important;">';
    htmlData += '<thead>';
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
            'url': '/previewBatchTransactionsList',
            'data': function (d) {
                return $.extend({}, d, {
                    "reference": batchReference,
                    "account": accountNo,
                    "status": status
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
        ]
    });
    table.on('order.dt search.dt', function () {
        table.column(0, {search: 'applied', order: 'applied'}).nodes().each(function (cell, i) {
            cell.innerHTML = i + 1;
        });
    }).draw();
}

function approveBatchTransactions(batchReference, accountNo) {
    $("#loading").show();
    $.ajax({
        url: "/processBatch",
        data: {reference: batchReference, accountNo: accountNo, eventCode: "IN_0011"},
        type: 'POST',
        success: function (response) {
            $("#loading").hide();
            if (response.responseCode == 0) {
                $("#successFlag").html('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert" aria-label="Close">x</button><strong>' + response.message + '</strong></div>').show();
                $('#bulkPayments').DataTable().ajax.reload();
            } else {
                if (response.message == undefined && response.responseCode == undefined)
                    $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert" aria-label="Close">x</button><strong>General error!</strong></div>').show();
                else
                    $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert" aria-label="Close">x</button><strong>' + response.responseCode + ' ' + response.message + ' error!</strong></div>').show();
            }
        },
        error: function (res) {
            $("#loading").hide();
            $('#errorMessage').after('<div class="row"><h4 class="error" style="color:red">Error: Could not initiate payment!</h4></div>');
        }
    });
}

function initiateBatch(batchReference, eventCode) {
    $("#loading").show();
    if ($("#transactionFile").val() == "") {
        $("#loading").hide();
        $("#modal-alert").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert" aria-label="Close">x</button><strong>File cannot be empty!</strong></div>').show();
        return;
    }
    $.ajax({
        url: "/processBatch",
        data: {reference: batchReference, eventCode: eventCode},
        type: 'POST',
        success: function (response) {
            $("#loading").hide();
            if (response.responseCode == 0) {
                $('#bulkPayments').DataTable().ajax.reload();
                uploadDoc(batchReference);
            } else {
                setTimeout(function() {
                    $("#myModal").modal('toggle');
                }, 500);
                if (response.message == undefined && response.responseCode == undefined)
                    $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert" aria-label="Close">x</button><strong>General error!</strong></div>').show();
                else
                    $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert" aria-label="Close">x</button><strong>' + response.responseCode + ' ' + response.message + ' error!</strong></div>').show();
            }
        },
        error: function (res) {
            $("#loading").hide();
            $('#errorMessage').after('<div class="row"><h4 class="error" style="color:red">Error: Could not initiate payment!</h4></div>');
        }
    });
}

function uploadDoc(batchReference) {
    let formData = new FormData();
    formData.append("file", $("#transactionFile")[0].files[0]);
    formData.append("reference", batchReference);
    $.ajax({
        url: "/uploadDoc",
        type: 'POST',
        enctype: 'multipart/form-data',
        processData: false,
        contentType: false,
        data: formData,
        success: function (res) {
            $("#loading").hide();
            $('.error').html('');
            if(res.result != -1) {
                $("#successFlag").html('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert" aria-label="Close"></button><strong>Successfully uploaded file!</strong></div>');
            }
            setTimeout(function() {
                $("#myModal").modal('toggle');
            }, 500);
        },
        error: function (res) {
            $("#loading").hide();
            $('#errorMessage').after('<div class="row"><h4 class="error" style="color:red">Error: Could not upload file!</h4></div>');
        }
    });
}

function previewSupportingDoc(batchReference, accountNo, status, userRoles) {
    var htmlData = '<div class="row"><div class="panel panel-default"><div class="panel-body">';
    htmlData += "<button onclick=\"previewBatchTransactions('"+batchReference+"', '"+accountNo+"', '"+status+"', '"+userRoles+"');\" class=\"previous btn btn-primary\">&#8249; Back</button>";
    htmlData += "<iframe class=\"col-md-12\" src=\"/previewMUSESupportingDocument?reference="+batchReference+"\" width=\"1300\" height=\"700\" frameborder=\"1\" scrolling=\"yes\"/>"
    htmlData += "</div></div></div>";
    $("#areaValue").html(htmlData);
}

function convertCurrency(batchReference, accountNo) {
    $.ajax({
        url: "/api/convertCurrency",
        type: 'POST',
        data: {batchReference: batchReference, account: accountNo},
        success: function (res) {
            $("#loading").hide();
            $('.error').html('');
            if (res.responseCode == 0) {
                $("#successFlag").html('<div class="alert alert-success" role="alert" id="successFlag"><button '+
                'type="button" class="close" data-dismiss="alert" aria-label="Close"></button><strong>Successfully '+
                'converted to TZS!</strong></div>');
            } else {
                $('#errorMessage').after('<div class="row"><h4 class="error" style="color:red">('+
                res.responseCode+') '+res.message+'!</h4></div>');
            }
            setTimeout(function() {
                $("#myModal").modal('toggle');
            }, 500);
        },
        error: function (res) {
            $("#loading").hide();
            $('#errorMessage').after('<div class="row"><h4 class="error" style="color:red">Error: Failed to convert currency!</h4></div>');
        }
    });
}