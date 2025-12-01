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

function selectAllTipsStatement() {
    var items = document.getElementsByName('statementTIPSTxns');
    for (var i = 0; i < items.length; i++) {
        if (items[i].type === 'checkbox')
            items[i].checked = true;
    }
}
function unselectAllTipsStatement() {
    var items = document.getElementsByName('statementTIPSTxns');
    for (var i = 0; i < items.length; i++) {
        if (items[i].type === 'checkbox')
            items[i].checked = false;
    }
}

function selectAllTipsLedgers() {
    var items = document.getElementsByName('ledgerTIPSTxns');
    for (var i = 0; i < items.length; i++) {
        if (items[i].type === 'checkbox')
            items[i].checked = true;
    }
}
function unselectAllTipsLedgers() {
    var items = document.getElementsByName('ledgerTIPSTxns');
    for (var i = 0; i < items.length; i++) {
        if (items[i].type === 'checkbox')
            items[i].checked = false;
    }
}

//set status of tips transactions to solve ambiguous
$('#ConfirmBulkTipsTxns').click(function () {
            var post_arr = [];
            var txnids = "'0'";
            // confirm the TIPS transactions either not in ledger or statement
            $('#tipsGeneralReconRepo input[type=checkbox]').each(function () {
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
                var exceptionType = $('#exceptionType').val();
                console.log(post_arr, exceptionType);
                $.ajax({
                    url: "/fireConfirmBulkTipsTxns",
                    data: {txnid: txnids, exceptionType:exceptionType},
                    type: 'POST',
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

function processTIPSBulkAmbiguous() {
    $("#ambigousTIPSBtn").hide();
    var exceptionType = $('#exceptionType').val();
    var post_arr = [];
    var hiddenData = "";
    var txndate;
    // Get checked checkboxes on thirdparty side
    $('#confirmTIPSStatementSide input[type=checkbox]').each(function () {
        if (jQuery(this).is(":checked")) {
            var id = this.id;
            var datas = id.split('==');
            hiddenData += '<input type="text"  name="exceptionType" value"'+exceptionType+'" hidden=""/>';
            hiddenData += '<input type="text"  name="txnid" value="' + datas[0] + '" hidden=""/>' + '<input type="text"  name="statementTxndate" value="' + datas[1] + '" hidden=""/>';
            var datas = id;
            post_arr.push({
                txnid: datas,
            });
        }
    });
    // Get checked checkboxes on ledger side
    $('#confirmTIPSLedgerSide input[type=checkbox]').each(function () {
        if (jQuery(this).is(":checked")) {
            var id = this.id;
            var datas = id.split('==');
            hiddenData += '<input type="text"  name="exceptionType" value"'+exceptionType+'" hidden=""/>';
            hiddenData += '<input type="text"  name="txnid" value="' + datas[0] + '" hidden=""/>' + '<input type="text"  name="leddgerSideTxndate" value="' + datas[1] + '" hidden=""/>';
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
                       <form role="form" name="tipsBulkConfirmation" id="tipsBulkConfirmation" enctype="multipart/form-data">
                         <div class="col-md-6">
                            <select class=" form-control" id="tipsGr" name="tipsAmbiguousOption" required>
                                    <option value="off" selected>--SELECT AMBIGUOUS OPTION--</option>
                                    <option value="1">MARK AS SUCCESS</option>
                            </select>
                          </div>
           ` + hiddenData + `
                        <div class="col-md-4"><button type="button" class="btn btn-primary" onclick="bulkTIPSAmbiguousConfimation()">Submit</button></div>
                    </div>`
                )
    } else {
        $("#ambigousTIPSBtn").show();
        alert('No data selected');
    }

}


//bulk coambiguous submission
function bulkTIPSAmbiguousConfimation() {
    var tipsAmbiguousOption = $('#tipsBulkConfirmation select[name=tipsAmbiguousOption]').val();
    if (tipsAmbiguousOption > 0) {
        var formdata = new FormData($("#tipsBulkConfirmation")[0]);
        console.log(formdata);
        $.ajax({
            url: '/fireSolveTIPSBulkAmbiguousTxns',
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
        alert('Please select Ambiguous TIPS Option');
    }
}
