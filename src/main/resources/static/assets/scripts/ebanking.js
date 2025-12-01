function selectAll() {
    var items = document.getElementsByName('cardDetails');
    for (var i = 0; i < items.length; i++) {
        if (items[i].type === 'checkbox')
            items[i].checked = true;
    }
}
function UnSelectAll() {
    var items = document.getElementsByName('cardDetails');
    for (var i = 0; i < items.length; i++) {
        if (items[i].type === 'checkbox')
            items[i].checked = false;
    }
}
/** CREATE NEW VISA CARD */
   function createNewCardRequest(){
    $('.create-ib-client-profile').attr('disabled', 'disabled');
    // Phone number validation
    const countryCode = $("#countryCode").val(); // e.g. +255
    const phone = $("#phone").val().trim();

    // Validate phone: must not start with 0 and must be digits only
    const phoneRegex = /^[1-9][0-9]{8}$/;

    if (!phoneRegex.test(phone)) {
        alert("❌ Invalid phone number.\n• Must be exactly 9 digits\n• Must not start with 0\n• Digits only");
        $("#phone").focus().addClass("is-invalid");
        $('.create-ib-client-profile').removeAttr('disabled');
        return;
    } else {
        $("#phone").removeClass("is-invalid");
    }
    var token = $("meta[name='_csrf']").attr("content");
              var header = $("meta[name='_csrf_header']").attr("content");
              var formdata = new FormData($('#createCardRequest')[0]);
               $.ajax({
                    url: "/createCardRequest",
                    type: 'POST',
                    data: formdata,
                    enctype: 'multipart/form-data',
                    processData: false,
                    contentType: false,
                    success: function (response) {
                                     $('.error').html('');
                                if(response.code ==200){
                                $('.create-ib-client-profile').attr('disabled', 'disabled');
                                    $("#successFlag").html('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>'+ response.data +'</strong></div>');
                                    $('.error').html(''); //clear error
                                    $("#createCardRequest").trigger("reset");//reset form
                                     $('#newClientForm').toggle('slide');
                                }else if((response.code ==51)||(response.code ==404)||(response.code ==26)||(response.code ==255)){
                                    $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>'+ response.data +'</strong></div>');
                                }else{
                                 $.each(response.data, function (key, value) {
                                    $('[name=' + key + ']').after('<span class="error text-danger">' + value + '</span>');
                                });

                                }
                            },
                    error: function(error){
                        console.log("error");
                    }
               });

            }

/** END CREATE VISA CARD */
function previewCardRequest(reference) {
    $("#loading").show();
    $.ajax({
        url: "/previewCardRequest",
        data: {reference: reference},
        type: 'POST',
        success: function (response) {
            $("#loading").hide();
            $("#myModal").modal("show");
            $("#areaValue").html(response);
        }
    });
}
//approve card request at branch level
function approveCardRequestBranchLevel(txnid) {
    $('#approveCardRequestBranch').attr('disabled', 'disabled');
    $('#returnVisaCardForAmmendment').attr('disabled', 'disabled');
    $("#loading").show();
    $.ajax({
        url: "/branchApproveCardCreationRequest",
        data: {reference: txnid},
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
            reloadDatatable("#profilesInitiated");
        }
    });
}

function HqPreviewCardOnHQWorkFlow(reference, accountNo, accountName, rimNo) {
    $("#loading").show();
    $.ajax({
        url: "/HqApproveCardRequestView",
        data: {reference: reference, accountName: accountName, accountNo: accountNo, rimNo: rimNo},
        type: 'POST',
        success: function (response) {
            $("#loading").hide();
            $("#myModal").modal("show");
            $("#areaValue").html(response);
        }
    });
}
//approve card request at branch level
function HqApproveCardRequestPANGeneration(reference, accountNo, accountName, rimNo, branchCode) {
    $('#approveCardRequestBranch').attr('disabled', true);
    $("#loading").show();
    $.ajax({
        url: "/HqApproveCardPANRequestGeneration",
        data: {reference:reference, accountNo: accountNo, accountName: accountName, rimNo: rimNo,branchCode:branchCode},
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
            reloadDatatable("#eftBulkPaymentsOnWorkFlow");
        }
    });
}

function HqCancelCardRequestPANGeneration(txnid, accountNo, accountName, rimNo) {
    $('#approveCardRequestBranch').attr('disabled', 'disabled');
    $("#loading").show();
    $.ajax({
        url: "/HqCancelCardPANRequestGeneration",
        data: {reference: txnid, accountNo: accountNo, accountName: accountName, rimNo: rimNo},
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
            reloadDatatable("#eftBulkPaymentsOnWorkFlow");
        }
    });
} 


function activateCardAndRequestInitialPIN(reference, accountNo, accountName) {
    $("#loading").show();
    $.ajax({
        url: "/HqApproveCardRequestView",
        data: {reference: reference, accountName: accountName, accountNo: accountNo},
        type: 'POST',
        success: function (response) {
            $("#loading").hide();
            $("#myModal").modal("show");
            $("#areaValue").html(response);
        }
    });
}
function receiveCardsFromPrintingUnit() {
    var PANList = "'0'";
    var post_arr = [];
    $('#cardManagement input[type=checkbox]').each(function () {
        if (jQuery(this).is(":checked")) {
            var id = this.id;
            var datas = id.split('==');
            PANList += ",'" + datas[0] + "'";
            post_arr.push({
                sourceAcct: datas[0]
            });
        }
    });
    if (post_arr.length > 0) {
        $.ajax({
            url: "/receiveCardsFromPrintingUnitModal",
            type: 'POST',
            data: {PANList: PANList},
            success: function (response) {
                $("#loading").hide();
                $("#myModal").modal("show");
                $("#areaValue").html(response);
            }
        });
    } else {
        alert('No data selected');
    }
}
//APPROVE RECEIPIENT OF CARDS FROM PRINTING UNIT
function submitReceiveCardsFromPrintingUnitForm() {
    $('#approveCardRequestBranch').attr('disabled', 'disabled');
    $("#loading").show();
    var formdata = new FormData($('#receiveCardsFromPrintingUnit')[0]);
    $.ajax({
        url: "/submitReceiveCardsFromPrintingUnitForm",
        type: 'POST',
        data: formdata,
        enctype: 'multipart/form-data',
        processData: false,
        contentType: false,
        success: function (response) {
            $("#loading").hide();
            $("#messageData").hide();
            if (response.result == 0) {
                $("#successFlag").after('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + '</strong></div>');
            } else {
                $("#successFlag").after('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + response.result + '</strong></div>');
            }
            $('#successFlag').delay(2000).show();
            reloadDatatable("#eftBulkPaymentsOnWorkFlow");
        }
    });
}
function dispatchCardsToBranches() {
    var PANList = "'0'";
    var post_arr = [];
    $('#cardManagement input[type=checkbox]').each(function () {
        if (jQuery(this).is(":checked")) {
            var id = this.id;
            var datas = id.split('==');
            PANList += ",'" + datas[0] + "'";
            post_arr.push({
                sourceAcct: datas[0]
            });
        }
    });
    if (post_arr.length > 0) {
        $.ajax({
            url: "/dipatchCardsTobranchesModal",
            type: 'POST',
            data: {PANList: PANList},
            success: function (response) {
                $("#loading").hide();
                $("#myModal").modal("show");
                $("#areaValue").html(response);
            }
        });
    } else {
        alert('No data selected');
    }
}
//

//APPROVE CARD DISPATCH
function submitDipatchCardsTobranchesForm(PANLists) {
    $('#approveCardRequestBranch').attr('disabled', 'disabled');
    $("#loading").show();
    $.ajax({
        url: "/submitDipatchCardsTobranchesForm",
        data: {PANLists: PANLists},
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
            reloadDatatable("#eftBulkPaymentsOnWorkFlow");
        }
    });
}
/*
 * ISSUE A CARD TO CUSTOMER
 */
//APPROVE CARD DISPATCH
function issueCardToCustomer(PANLists) {
    $('#approveCardRequestBranch').attr('disabled', 'disabled');
    $("#loading").show();
    $.ajax({
        type: 'POST',
        url: "/issueCardToCustomerModal",
        data: {PANList: PANLists},
        success: function (response) {
            $("#loading").hide();
            $("#myModal").modal("show");
            $("#areaValue").html(response);
        }
    });
}
//APPROVE CARD DISPATCH
function submitCardIssunceModalForm() {
    $('#issueCardToCoreBankingBtn').attr('disabled', 'disabled');
    $("#loading").show();
    var formdata = new FormData($('#issueCardToCoreBanking')[0]);
    $.ajax({
        url: "/submitCardIssunceModalForm",
        type: 'POST',
        data: formdata,
        enctype: 'multipart/form-data',
        processData: false,
        contentType: false,
        success: function (response) {
            $("#loading").hide();
            $("#messageData").hide();
            if (response.result == 0) {
                $("#successFlag").html('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + '</strong></div>');
            }else if(response.result == 51) {
                $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + 'This account has Insuffient Balance, Advice customer to deposit. Code: ' + response.result + '</strong></div>');
            }else if(response.result == 57){
                $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + 'This account is BLOCKED from debiting it, Please contact Ebanking team. Code: ' + response.result + '</strong></div>');
            }else if(response.result == 26){
                $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + 'This card is already issued, please contact Ebanking team to confirm. Code: ' + response.result + '</strong></div>');
            }else{
                 $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + response.result + '</strong></div>');
            }
            $('#successFlag').delay(2000).show();
            reloadDatatable("#eftBulkPaymentsOnWorkFlow");
        }
    });
}


//change card collecting branch
function updateCardCollectingBranch(){
    $("#loading").show();
    var pan = $("#cardPan").val();
     var cardReference = $("#cardReference").val();
     var formdata = new FormData($("#form")[0]);
     formdata.append("pan",pan);
     formdata.append("cardReference",cardReference);
    $.ajax({
        url:"/updateCardCollectingBranchAjax",
        data:formdata,
        type:"POST",
        processData:false,
        contentType:false,
        success: function (response) {
        console.log(response);
                    $("#loading").hide();
                    if (response==="SUCCESS") {
                        $("#successFlag").html('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response + '</strong></div>');
                    } else {
                        $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response + '</strong></div>');
                    }
                    $('#successFlag').delay(2000).show();
                    reloadDatatable("#cardManagement");
           }
    });
}


//change customer phone number
function updateCustomerPhone(){
    $("#loading").show();
    var pan = $("#cardPan").val();
    var cardReference = $("#cardReference").val();
    var formdata = new FormData($("#form")[0]);
        formdata.append("pan",pan);
        formdata.append("cardReference",cardReference);
    $.ajax({
        url:"/updateCustomerPhoneAjax",
        data:formdata,
        type:"POST",
        processData:false,
        contentType:false,
        success: function (response) {
        console.log(response);
                    $("#loading").hide();
//                    $("#messageData").hide();
                    if (response==="SUCCESS") {
                        $("#successFlag").html('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>Contact updated successifully</strong></div>');
                    } else {
                        $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response + '</strong></div>');
                    }
                    $('#successFlag').delay(2000).show();
                    reloadDatatable("#cardManagement");

           }
    });
}

//change card collecting branch
function rejectCard(){
    $("#loading").show();
    var pan = $("#cardPan").val();
    var cardRejectingReason = $("#cardRejectingReason").val();
    var formdata = new FormData($("#form")[0]);
    var cardReference = $("#cardReference").val();
    formdata.append("pan",pan);
    formdata.append("cardReference",cardReference);
    $.ajax({
        url:"/rejectVisaCardAjax",
        data:formdata,
        type:"POST",
        processData:false,
        contentType:false,
        success: function (response) {
        console.log(response);
                    $("#loading").hide();
//                    $("#messageData").hide();
                    if (response==="SUCCESS") {
                        $("#successFlag").html('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong> Card rejected successifully </strong></div>');
                    } else {
                        $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response + '</strong></div>');
                    }
                    $('#successFlag').delay(2000).show();


           }
    });

}

/** change PAN number **/
function changePan(){
    $("#loading").show();
    var pan = $("#cardPan").val();
    var cardReference = $("#cardReference").val();
    var formdata = new FormData($("#form")[0]);
    formdata.append("pan",pan);
    formdata.append("cardReference",cardReference);
    $.ajax({
        url:"/updateCardPanAjax",
        data:formdata,
        type:"POST",
        processData:false,
        contentType:false,
        success: function (response) {
                    $("#loading").hide();
//                    $("#messageData").hide();
                    if (response==="SUCCESS") {
                        $("#successFlag").html('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>Card PAN updated successifully</strong></div>');
                    } else {
                        $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response + '</strong></div>');
                    }
                    $('#successFlag').delay(2000).show();

           }
    });
}
/**END UPDATE PAN */

$('#actionTrigger').change(function(){
    var action = $(this).val();

    if(action==="updateBranch"){
        $('.updateBranchD').show();
        $('.updatePhoneD').hide();
        $('.changePanD').hide();
        $('.rejectCardD').hide();
    }else if(action==="updatePhone"){
         $('.updateBranchD').hide();
         $('.changePan').hide();
         $('.rejectCardD').hide();
         $('.updatePhoneD').show();
    }else if(action === "rejectCard"){
        $('.updateBranchD').hide();
        $('.updatePhoneD').hide();
        $('.changePanD').hide();
        $('.rejectCardD').show();
    }else{
         $('.updateBranchD').hide();
         $('.updatePhoneD').hide();
         $('.rejectCardD').hide();
         $('.rejectCardD').hide();
        $('.changePanD').show();
    }
});

/**PREVIEW CARD DETAILS*/
function previewVisaDetails(accountNo,rimNo,reference,serviceType){
$("#loading").show();
    $.ajax({
        type: 'POST',
        url: "/firePreviewVisaDetails",
        data: {accountNo: accountNo, rimNo: rimNo,reference: reference, serviceType:serviceType},
        success: function (response) {
            $("#loading").hide();
            $("#myModal").modal("show");
            $("#areaValue").html(response);
        }
    });
}



//stateless things
function statelessVisaCardUpdating(){
    $("#loading").show();
    var accountNo = $("#accountNo").val();
     var cardReference = $("#cardReference").val();
     var selectedValue = $("#statelessActionTrigger").val();
     var enteredValue = $("#statelessUpdating").val();
     var formdata = new FormData();
     formdata.append("accountNo",accountNo);
     formdata.append("cardReference",cardReference);
     formdata.append("selectedValue",selectedValue);
     formdata.append("enteredValue",enteredValue);
        $.ajax({
            url:"/fireStatelessVisaCardUpdatingAjax",
            data:formdata,
            type:"POST",
            processData:false,
            contentType:false,
            success: function (response) {
            console.log(response);
                        $("#loading").hide();
                        if (response==="SUCCESS") {
                            $("#successFlag").html('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response + '</strong></div>');
                        } else {
                            $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response + '</strong></div>');
                        }
                        $('#successFlag').delay(2000).show();
                        reloadDatatable("#cardManagement");
               }
        });

}


/** AMEND VISA CARD*/
function previewAmendmentCardRequest(reference,customerAcctNo) {
    $("#loading").show();
    $.ajax({
        url: "/firePreviewAmendVisaCard",
        data: {reference: reference, customerAcctNo:customerAcctNo},
        type: 'POST',
        success: function (response) {
            $("#loading").hide();
            $("#myModal").modal("show");
            $("#areaValue").html(response);
        }
    });
}

/** UPDATE VISA CARD */
   function updateVisaCardReq(){
              var token = $("meta[name='_csrf']").attr("content");
              var header = $("meta[name='_csrf_header']").attr("content");
              var formdata = new FormData($('#form222')[0]);
              $("#preloader2").show();
               $.ajax({
                    url: "/fireUpdateVisaCardReq",
                    type: 'POST',
                    data: formdata,
                    enctype: 'multipart/form-data',
                    processData: false,
                    contentType: false,
                    success: function (response) {
                            $("#preloader2").hide();
                    console.log(response);
                                     $('.error').html('');
                                if(response.code ==200){
                                $('.update-ib-client-profile').attr('disabled', 'disabled');
                                    $("#successFlag").html('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>'+ response.data +'</strong></div>');
                                    $('.error').html(''); //clear error
                                }else if((response.code ==404)){
                                    $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>'+ response.data +'</strong></div>');
                                }else{
                                 $.each(response.data, function (key, value) {
                                    $('[name=' + key + ']').after('<span class="error text-danger">' + value + '</span>');
                                });

                                }
                            },
                    error: function(error){
                         $("#preloader2").hide();
                        console.log("error");
                    }
               });

            }

function generateNewVisaPin(accountNo,customerRim,customerShortName,pan,phone,checksum) {
    $("#loading").show();
    $("#generateNEWPIN").attr('disabled','disabled');
    $.ajax({
        url: "/fireGenerateNewVisaPin",
        data: {accountNo: accountNo, customerRim:customerRim, customerShortName: customerShortName, pan: pan, phone: phone, checksum: checksum},
        type: 'POST',
        success: function (response) {
            $("#loading").hide();
           console.log(response.responseCode);
           if(response.responseCode == "0"){
              $("#successFlag").html('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>'+ response.message +'</strong></div>');
           }else{
              $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>'+ response.message +'</strong></div>');
           }
        }
    });
}
