
 $(document).ready(function () {
       $(".select2").select2({ width: '100%' });
       $('.comma-number').keyup(function (event) {
          // skip for arrow keys
          if (event.which >= 37 && event.which <= 40) {
               event.preventDefault();
            }

            $(this).val(function (index, value) {
               value = value.replace(/,/g, '');
                 return numberWithCommas(value);
              });
            });
  });
    function numberWithCommas(x) {
              var parts = x.toString().split(".");
              parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ",");
              return parts.join(".");
          }

          function getAccountDetails() {
                      $("#loading").show();
                      var accountNo = $("#accNumber").val();
                      $.ajax({
                          url: "/fireQueryPensionerAcctDetails",
                          type: 'GET',
                          data: {accountNo: accountNo, code: "searchAccount"},
                          dataType: "json",
                          success: function (res) {
                              $("#loading").hide();
                              $("#rubikonActNM").val(res[0].CUST_NM);
//                              console.log(res)
                          },
                          error: function (res) {
                              $("#loading").hide();
                              $('#senderAccount').after('<h4 class="error" style="color:red     font-size: 8px !important;"> Error: Please Try Again. Please contact IT Helpdesk / Database Administrator: RUBIKON NOT FOUND</h4>');
                              console.log(res);
                          }
                      })
                  }
/** VIEW PENSIONER STATEMENTS*/

function viewPensionerStatement(pensionID,ssn,fullName,dob,retirementDate,mpAmount,bankAccount,bankName,pensionerStatus,statusReason,pensionEndDate,lastPayment,hasLoan,phone,pensionMode) {
 iziToast.success({ title: 'SUCCESS', message: 'Processing Data', color:'green',position:'topRight', timeout: 5000 });
                      var count = 0;
                        $.ajax({
                            'url': '/fireviewPensionerStatementModalAjax',
                            data: {pensionID: pensionID, ssn: ssn,fullName: fullName,dob: dob,retirementDate: retirementDate,mpAmount: mpAmount,bankAccount: bankAccount,bankName: bankName,pensionerStatus: pensionerStatus,statusReason: statusReason,pensionEndDate: pensionEndDate,lastPayment: lastPayment,hasLoan: hasLoan,phone: phone,pensionMode: pensionMode},
                            type: "POST",
                            success: function (response) {
                            $("#myModal").modal("show");
                                        // Decode Base64 to binary and show some information about the PDF file (note that I skipped all checks)
                                        var bin = atob(response.jasper);
                                        console.log('File Size:', Math.round(bin.length / 1024), 'KB');
                                        console.log('PDF Version:', bin.match(/^.PDF-([0-9.]+)/)[1]);

                                        // Embed the PDF into the HTML page and show it to the user
                                        var obj = document.createElement('object');
                                        obj.style.width = '100%';
                                        obj.style.height = '842pt';
                                        obj.type = 'application/pdf';
                                        obj.data = 'data:application/pdf;base64,' + response.jasper;

                                        // Insert a link that allows the user to download the PDF file
                                        var link = document.createElement('a');
                                        link.innerHTML = 'Download PDF file';
                                        link.download = 'file.pdf';
                                        link.href = 'data:application/octet-stream;base64,' + response.jasper;

                                        var htmlData = '<div class="row">';
                                        htmlData += '<div class="panel panel-default panel-fill" ><div class="panel-heading no-print"><h4 class="panel-title">PSSSF PENSIONER STATEMENT</h4>';

                                        htmlData += '</div>';
                                        htmlData += '<div class="panel-body">';
                                        htmlData += '<div class="col-md-12 pdf-content"></div>';
                                        htmlData += '</div>';
                                        htmlData += '</div>';
                                        htmlData += '</div>';
                                        $("#areaValue").html(htmlData);
                                        $(".pdf-content").append(obj);
                            },
                            error: function (e) {
                                $("#loading").hide();
                                console.log(e);
                            }
                        }); //end ajax
}


function pensionerLoanRequest(pensionerID,pensionerName) {
//   console.log(pensionerID);
   $.ajax({
         url: "/firePensionerLoanRequestAjax",
         data: {pensionerID: pensionerID, pensionerName: pensionerName },
         type: 'POST',
         success: function (response) {
         $("#loading").hide();
         $("#myModal").modal("show");
         $("#areaValue").html(response);
          }
     });
}

/** verifyPensionerLoan*/
function verifyPensionerLoan(){
  $('#loanVerficationReq').attr('disabled', 'disabled');
 iziToast.success({ title: 'PROCESSING', message: 'Sending Loan Verification', color:'green',position:'topRight', timeout: 5000 });
    var token = $("meta[name='_csrf']").attr("content");
    var header = $("meta[name='_csrf_header']").attr("content");
    var formdata = new FormData($('#verifyLoan')[0]);
    $.ajax({
       processData: false,
       contentType: false,
       data: formdata,
       type: "POST",
       url: "/fireverifyPensionerLoanAjax",
       beforeSend: function (request) {
            request.setRequestHeader(header, token);
       },
       success: function (response) {
            console.log(response.code);
            $('.error').html('');
            if(response.code == 1){
              $('#loanVerficationReq').attr('disabled', 'disabled');
              $("#successFlag").html('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>PENSIONER LOAN VERIFICATION INITIATED SUCCESSFULLY</strong></div>');
              $('.error').html(''); //clear error
              }else if((response.code ==0)||(response.code ==404)){
                   $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>PENSIONER LOAN VERIFICATION FAILED</strong></div>');
              }else{
                $.each(response.data, function (key, value) {
                   $('[name=' + key + ']').after('<span class="error text-danger">' + value + '</span>');
                });
             }
       },
      error: function (e) {
                    console.log(e);
                  }
    });
}

function previewLoanReqForVerification(reference) {
 iziToast.success({ title: 'PROCESSING', message: 'Please wait', color:'green',position:'topRight', timeout: 5000 });
    $.ajax({
        url: "/firePreviewLoanReqForVerification",
        data: {reference: reference},
        type: 'POST',
        success: function (response) {
            $("#loading").hide();
            $("#myModal").modal("show");
            $("#areaValue").html(response);
        }
    });
}


/*Authorize Loan Verification Request to PSSSF**/
    function fireAuthLoanReqToPensioners(reference){
    $('#authLoanBtn').prop('disabled','disabled');
//     $("#loading").show();
        iziToast.success({ title: 'PROCESSING', message: 'Sending Loan Verification Authorization Request', color:'green',position:'topRight', timeout: 5000 });
         $.ajax({
             url: "/fireAuthLoanRequestToPensioners",
             data: {reference: reference},
             type: 'POST',
             success: function (response) {
                         $("#loading").hide();
                         $("#messageData").hide();
                         if (response.result == 200) {
                             $("#successFlag").after('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + '</strong></div>');
                             $('#dtableVerification').DataTable().ajax.reload();
                         } else {
                             $("#successFlag").after('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + response.result + '</strong></div>');
                         }
                         $('#successFlag').delay(2000).show();
                     }
         });
    }

    /**RETURN LOAN REQ FOR AMENDMENT**/
    function fireReturnAmendLoanReq(reference){
    $('#returnForAmmendmentLoanBtn').prop('disabled','disabled');
    $('#authLoanBtn').prop('disabled','disabled');
//     $("#loading").show();
        iziToast.success({ title: 'RETURNING', message: 'Returning Loan Request for Amendment', color:'green',position:'topRight', timeout: 3000 });
         $.ajax({
             url: "/fireReturnAmendLoanReq",
             data: {reference: reference},
             type: 'POST',
             success: function (response) {
                         $("#messageData").hide();
                         if (response.responseCode == 0) {
                             $("#successFlag").after('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + '</strong></div>');
                             $('#dtableAmend').DataTable().ajax.reload();
                         } else {
                             $("#successFlag").after('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + response.result + '</strong></div>');
                         }
                         $('#successFlag').delay(2000).show();
                     }
         });
    }

    /* CHANGE OF ACCOUNT**/
    function pensionerChangeAcct(pensionerID) {
     iziToast.success({ title: 'PROCESSING', message: 'Please wait', color:'green',position:'topRight', timeout: 5000 });
        $.ajax({
            url: "/firePensionerChangeAcctModal",
            data: {pensionerID: pensionerID},
            type: 'POST',
            success: function (response) {
                $("#loading").hide();
                $("#myModal").modal("show");
                $("#areaValue").html(response);
            }
        });
    }

    /**AMEND REQUEST**/
    function previewLoanReqForAmendment(reference) {
     iziToast.success({ title: 'PROCESSING', message: 'Please wait', color:'green',position:'topRight', timeout: 5000 });
        $.ajax({
            url: "/firePreviewLoanReqForAmendment",
            data: {reference: reference},
            type: 'POST',
            success: function (response) {
                $("#loading").hide();
                $("#myModal").modal("show");
                $("#areaValue").html(response);
            }
        });
    }


    /** Re submit the amended loan req*/
    function fireReSubmitLoanReq(){
      $('#resubmitLoanBtn').attr('disabled', 'disabled');
     iziToast.success({ title: 'AMENDING', message: 'Sending Amendment Data ', color:'green',position:'topRight', timeout: 3000 });
        var token = $("meta[name='_csrf']").attr("content");
        var header = $("meta[name='_csrf_header']").attr("content");
        var formdata = new FormData($('#amendLoan')[0]);
        $.ajax({
           processData: false,
           contentType: false,
           data: formdata,
           type: "POST",
           url: "/fireReSubmitLoanReqAjax",
           beforeSend: function (request) {
                request.setRequestHeader(header, token);
           },
           success: function (response) {
                console.log(response);
                $('.error').html('');
                if(response.code == 0){
                  $('#resubmitLoanBtn').attr('disabled', 'disabled');
                  $("#successFlag").html('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>'+response.data+'</strong></div>');
                  $('#dtableAmend').DataTable().ajax.reload();
                  $('.error').html(''); //clear error
                  }else if(response.code ==99){
                       $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>'+ response.data +'</strong></div>');
                  }else{
                    $.each(response.code, function (key, value) {
                       $('[name=' + key + ']').after('<span class="error text-danger">' + value + '</span>');
                    });
                 }
           },
          error: function (e) {
                        console.log(e);
                      }
        });
    }
