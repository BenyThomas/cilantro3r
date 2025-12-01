function replayEftMessage() {
    $("#ambigousBtn").hide();

    var post_arr = [];
    var hiddenData = "";

    $('#inwardEFTBankSuccessSummary input[type=checkbox]').each(function () {
        if (jQuery(this).is(":checked")) {
            var id = this.id;
            var datas = id.split('==');
            hiddenData += '<input type="text"  name="identifier" value="existOnThirdParty" hidden=""/>';
            hiddenData += '<input type="text"  name="txnid" value="' + datas[0] + '" hidden=""/>';
            hiddenData += '<input type="text"  name="thirdpartyTxndate" value="' + datas[1] + '" hidden=""/>';
            post_arr.push({ txnid: id });
        }
    });

    if (post_arr.length === 0) {
        $("#ambigousBtn").show();
        alert('No data selected');
        return;
    }

    $("#actionProcessed").hide();

    // Build the form with both sections (branch + stawi) and toggle via JS
    var formHtml = `
      <div class="row">
        <form role="form" name="replayEFTMessage" id="replayEFTMessage" enctype="multipart/form-data">
          <div class="col-md-3">
            <select class="form-control" id="refundop" name="ambiguousOption" required>
              <option value="off" selected>--SELECT REPLAY OPTION--</option>
              <option value="1">TO  HQ EFT TANSFER AWAITING</option>
              <option value="2">TO TANESCO SACCOS COLLECTION ACCT</option>
              <option value="3">TO BRANCH EFT AWAITING</option>
              <option value="4">TO INSURANCE HQ COMMISSION</option>
              <option value="5">REPLAY TO INTENDED</option>
              <option value="6">REPLAY TO AIRTEL DGS GL</option>
              <option value="7">REPLAY TO TIGO DGS GL</option>
              <option value="8">REPLAY TO HALOTEL DGS GL</option>
              <option value="9">REPLAY TO MKOBA DGS GL</option>
              <option value="10">MTN-STAWI BOND</option>
            </select>
          </div>

          <div class="col-md-3" id="branchField">
            <input class="form-control" id="branchNo" name="branchNo" required placeholder="Branch Number eg. 173">
          </div>

          <!-- STAWI-ONLY FIELDS -->
          <div class="col-md-6" id="stawiFields" style="display:none;">
            <div class="form-group">
              <textarea class="form-control" id="replayReason" name="reason" rows="2" placeholder="Replay reason"></textarea>
            </div>

            <div class="form-group">
              <label for="cdsNumber" style="display:block;">CDS Number</label>
              <div class="input-group">
                <input class="form-control" id="cdsNumber" name="cdsNumber" placeholder="e.g. 730019">
                <span class="input-group-btn">
                  <button type="button" class="btn btn-warning" id="verifyCdsBtn">Verify</button>
                </span>
              </div>
              <div id="lookupResult" style="margin-top:8px;"></div>
            </div>
          </div>

          ${hiddenData}

          <div class="col-md-4" id="submitContainer">
            <button type="button" class="btn btn-primary btn-bulkAmbiguous" id="submitReplayBtn" onclick="processEftReplay()">Post</button>
          </div>
        </form>
      </div>
    `;

    document.querySelector('#content').insertAdjacentHTML('afterbegin', formHtml);

    // attach handlers (delegated)
    $(document).off('change', '#refundop').on('change', '#refundop', function () {
        toggleStawiUI(this.value);
    });
    $(document).off('click', '#verifyCdsBtn').on('click', '#verifyCdsBtn', function () {
        verifyStawiAccount();
    });
}

function processEftReplay() {
    var formdata = new FormData($('#replayEFTMessage')[0]);
    console.log('REPLAY REASONS: ' + formdata);
    $.ajax({
        url: "/replayEFTTxnsToCoreBanking",
        type: 'POST',
        data: formdata, //{senderAccount: senderAccount, senderName: senderName, senderAddress: senderAddress, amount: amount, currency: currency, beneficiaryName: beneficiaryName, beneficiaryBIC: beneficiaryBIC, description: description, supporting_doc: supporting_doc, beneficiaryAccount: beneficiaryAccount},
        enctype: 'multipart/form-data',
        processData: false,
        contentType: false,
        success: function (response) {
            if (response.result == 0) {
                $("#successFlag").html('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + '</strong></div>').show();
            } else {
                $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + response.result + '</strong></div>').show();
            }
        },
        error: function (response) {
            if (response.result == 0) {
                $("#successFlag").html('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + '</strong></div>').show();
            } else {
                $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + response.result + '</strong></div>').show();
            }
        }
    });
}
function toggleStawiUI(value) {
    var isStawi = (value === '10');

    if (isStawi) {
        // Show only reason + cds; hide branch; disable Post until verified
        $('#stawiFields').show();
        $('#branchField').hide();
        $('#branchNo').prop('required', false).val('');

        $('#submitReplayBtn').prop('disabled', true); // enable only after SUCCESS verify
    } else {
        // Normal flow
        $('#stawiFields').hide();
        $('#lookupResult').empty();
        $('#branchField').show();
        $('#branchNo').prop('required', true);

        $('#submitReplayBtn').prop('disabled', false);
    }
}

function verifyStawiAccount() {
    var cds = ($('#cdsNumber').val() || '').trim();
    if (!cds) {
        $('#lookupResult').html('<div class="alert alert-warning">Please enter a CDS Number first.</div>');
        return;
    }

    // UX
    $('#verifyCdsBtn').prop('disabled', true).text('Verifying…');
    $('#lookupResult').html('');

    // POST to your backend proxy that calls the DSE lookup
    // (Example endpoint; implement Controller below.)
    $.ajax({
        url: '/stawi/lookup',                // <-- your backend endpoint
        type: 'POST',
        contentType: 'application/json',
        data: JSON.stringify({ dseAccount: cds }),
        success: function (res) {
            // Expecting {status, message, response:{ investorName, investorPhoneNumber, ... }}
            if (res && res.status === 'SUCCESS') {
                var r = res.response || {};
                var name = r.investorName || 'N/A';
                var phone = r.investorPhoneNumber || 'N/A';
                $('#lookupResult').html(
                    '<div class="alert alert-success">' +
                    '<strong>Verified.</strong> Investor: ' + name + ' | Phone: ' + phone +
                    '</div>'
                );
                $('#submitReplayBtn').prop('disabled', false); // allow Post
            } else {
                $('#lookupResult').html(
                    '<div class="alert alert-danger"><strong>Verification failed:</strong> ' +
                    (res && res.message ? res.message : 'Unknown error') +
                    '</div>'
                );
                $('#submitReplayBtn').prop('disabled', true);
            }
        },
        error: function (xhr) {
            var msg = (xhr.responseText || '').toString();
            $('#lookupResult').html(
                '<div class="alert alert-danger"><strong>Verification error:</strong> ' + msg + '</div>'
            );
            $('#submitReplayBtn').prop('disabled', true);
        },
        complete: function () {
            $('#verifyCdsBtn').prop('disabled', false).text('Verify');
        }
    });
}

function downloadEftOutwardReport(reference, type) {

    $.ajax({
        url: "/downloadEftOutwardReport",
        type: 'GET',
        data: {batchReference: reference, reportFormat: type}, //{senderAccount: senderAccount, senderName: senderName, senderAddress: senderAddress, amount: amount, currency: currency, beneficiaryName: beneficiaryName, beneficiaryBIC: beneficiaryBIC, description: description, supporting_doc: supporting_doc, beneficiaryAccount: beneficiaryAccount},
        success: function (response) {
            if (response.result == 0) {
                $("#successFlag").html('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + '</strong></div>').show();
            } else {
                $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + response.result + '</strong></div>').show();
            }
        },
        error: function (response) {
            if (response.result == 0) {
                $("#successFlag").html('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + '</strong></div>').show();
            } else {
                $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + response.result + '</strong></div>').show();
            }
        }
    });
}

//RETURN TRANSACTIONS WITH REASON
function returnTransactionWithReason() {
    $("#ambigousBtn").hide();
    var post_arr = [];
    var hiddenData = "";
    var references = "'0'";
    var accounts = "";
    var txndate;
    // Get checked checkboxes on thirdparty side
    $('#inwardEFTBankSuccessSummary input[type=checkbox]').each(function () {
        if (jQuery(this).is(":checked")) {
            var id = this.id;
            var datas = id.split('==');
            references += ",'" + datas[0] + "'";
            accounts += ",'" + datas[1] + "'";
            hiddenData += '<input type="text"  name="reference" value="' + datas[0] + '" hidden=""/>' + '<input type="text"  name="sourceAccount" value="' + datas[1] + '" hidden=""/>';
            var datas = id;
            post_arr.push({
                txnid: datas,
            });
        }
    });
    if (post_arr.length > 0) {
        $("#actionProcessed").hide();
        document.querySelector('#content').insertAdjacentHTML(
                'afterbegin',
                `<div class="row">
                      <form role="form" name="returnEftTransactionForm" id="returnEftTransactionForm" enctype="multipart/form-data">
                       <div class="col-md-3">
                                <select class=" form-control" id="refundop" name="returnReason" required>
                                <option value="off" selected>--SELECT RETURN OPTION--</option>
                                <option value="SUSP">SUSPICIOUS</option>
                                <option value="AC01">INCORECT ACCOUNT</option>
                                <option value="AC04">ACCOUNT IS CLOSED</option>
                                <option value="AC06">ACCOUNT IS BLOCKED</option>       
                            </select>
                        </div>
                        <div class="col-md-3">
                              <textarea id="reason" name="reason" placeholder="Return reason" rows="2" cols="36"></textarea>
                        </div>
                            <input type="text"  name="reference" value="` + references + `" hidden=""/>
                            <input type="text"  name="accounts" value="` + accounts + `" hidden=""/>

                          <div class="col-md-4"><button type="button" class="btn btn-primary btn-bulkAmbiguous" onclick="processReturnTransactionToThirdparty()">Submit</button></div>
                      </div>`
                )
    } else {
        $("#ambigousBtn").show();
        alert('No data selected');
    }

}
function processReturnTransactionToThirdparty() {
    var formdata = new FormData($('#returnEftTransactionForm')[0]);
    console.log('RETURN REASONS: ' + formdata);
    $.ajax({
        url: "/returnInwardEftTransactions",
        type: 'POST',
        data: formdata, //{senderAccount: senderAccount, senderName: senderName, senderAddress: senderAddress, amount: amount, currency: currency, beneficiaryName: beneficiaryName, beneficiaryBIC: beneficiaryBIC, description: description, supporting_doc: supporting_doc, beneficiaryAccount: beneficiaryAccount},
        enctype: 'multipart/form-data',
        processData: false,
        contentType: false,
        success: function (response) {
            if (response.result == 0) {
                $("#successFlag").html('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + '</strong></div>').show();
            } else {
                $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + response.result + '</strong></div>').show();
            }
        },
        error: function (response) {
            if (response.result == 0) {
                $("#successFlag").html('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + '</strong></div>').show();
            } else {
                $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + response.result + '</strong></div>').show();
            }
        }
    });
}


function pushTransactionToTach(reference,sourceAcct,destinationAcct) {
// iziToast.success({ title: 'PROCESSING', message: 'Sending Transaction To Tach', color:'green',position:'topRight', timeout: 3500 });
    $.ajax({
        url: "/firePushTransactionToTachAjax",
        data: {reference: reference, sourceAcct: sourceAcct, destinationAcct: destinationAcct},
        type: 'POST',
        success: function (response) {
             console.log(response.responseCode);
            if (response.responseCode == 0) {
                iziToast.success({ title: 'SUCCESS', message: 'Transaction Pushed To Tach', color:'green',position:'topRight', timeout: 3500 });
                 reloadDatatable("#dtable1");
            } else {
                iziToast.error({ title: 'FAIL', message: 'Failed To Push Transaction', color:'red',position:'topRight', timeout: 5000 });
            }
        }
    });
}


function cancelEftTransaction(reference,sourceAcct,destinationAcct) {
// iziToast.success({ title: 'PROCESSING', message: 'Sending Transaction To Tach', color:'green',position:'topRight', timeout: 3500 });
    $.ajax({
        url: "/fireCancelEftTransactionAjax",
        data: {reference: reference, sourceAcct: sourceAcct, destinationAcct: destinationAcct},
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

