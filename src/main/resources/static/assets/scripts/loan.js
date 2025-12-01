function previewCustomerLoans(accountNumber) {
                    $("#loading").show();
                      var count = 0;
                        $.ajax({
                            'url': '/firePreviewCustomerLoansAjax',
                            data: {accountNumber: accountNumber},
                            type: "POST",
                            success: function (response) {
                            var htmlDataT = '<div class="row"><div class="panel panel-default"><div class="panel-body"><div id="modal-alert"></div>';
                                htmlDataT += '<table id="customerLoans" class="display nowrap table table-striped table-bordered" style="width: 100%; font-size: 9.5px !important;">';
                                htmlDataT += '<thead>';
                                htmlDataT += '<tr>';
                                htmlDataT += '<th>S/N</th>';
                                htmlDataT += '<th>CUSTOMER NAME</th>';
                                htmlDataT += '<th>ACCOUNT</th>';
                                htmlDataT += '<th>CUSTOMER RIM</th>';
                                htmlDataT += '<th>LOAN ID</th>';
                                htmlDataT += '<th>REFERENCE</th>';
                                htmlDataT += '<th>PHONE</th>';
                                htmlDataT += '<th>DEVICE ID</th>';
                                htmlDataT += '<th>PURPOSE</th>';
                                htmlDataT += '<th>PRINCIPAL AMOUNT</th>';
                                htmlDataT += '<th>PROCESSING FEE</th>';
                                htmlDataT += '<th>INTEREST</th>';
                                htmlDataT += '<th>INDEMNITY</th>';
                                htmlDataT += '<th>STATUS</th>';
                                htmlDataT += '<th>COMMENTS</th>';
                                htmlDataT += '<th>REPAYMENT STATUS</th>';
                                htmlDataT += '<th>REPAYMENT REFERENCE</th>';
                                htmlDataT += '<th>REPAYMENT AMOUNT</th>';
                                htmlDataT += '<th>FEE BALANCE AMOUNT</th>';
                                htmlDataT += '<th>FEE REPAYEMENT STATUS</th>';
                                htmlDataT += '<th>PRINCIPAL REPAYMENT AMOUNT</th>';
                                htmlDataT += '<th>PRINCIPAL REPAYEMENT REF</th>';
                                htmlDataT += '<th>PRINCIPAL BALANCE AMOUNT </th>';
                                htmlDataT += '<th>PRINCIPAL REPAYMENT STATUS </th>';
                                htmlDataT += '</tr>';
                                htmlDataT += '</thead>';
                                htmlDataT += '<tbody></tbody>';
                                htmlDataT += '</table>';
                                htmlDataT += '</div></div></div>';
                                $("#loading").hide();
                                $("#myModal").modal("show");
                                $("#areaValue").html(htmlDataT);
                                    var table2 = $('#customerLoans').DataTable({
                                        dom: 'Blfrtip',
                                        "info": true,
                                        "bDestroy": true,
                                        lengthMenu: [10, 25, 50, 100, 200, 300, 500, 1000],
                                        scrollX: true,
                                        buttons: ['copy', 'csv', 'excel', 'pdf', 'print'],
                                        "data": response,
                                        columns: [
                                             {"data": "count",
                                              "render":function(data, type,row,meta){
                                                    return count +=1;
                                              }
                                              },
                                                         {"data": "fullName"},
                                                         {"data": "account"},
                                                         {"data": "customerRim"},
                                                         {"data": "loanId"},
                                                         {"data": "reference"},
                                                         {"data": "phoneNo"},
                                                         {"data": "deviceId"},
                                                         {"data": "purpose"},
                                                         {"data": "principalAmount",
                                                               "render": function numberWithCommas(principalAmount) {
                                                                return principalAmount.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
                                                         }
                                                         },
                                                         {"data": "processingFee",
                                                          "render": function numberWithCommas(processingFee) {
                                                             return processingFee.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
                                                          }
                                                          },
                                                          {"data": "interest"},
                                                          {"data": "indemnity",
                                                          "render": function numberWithCommas(indemnity) {
                                                             return indemnity.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
                                                           }
                                                           },
                                                         {"data": "status"},
                                                         {"data": "comments"},
                                                         {"data": "repaymentStatus"},
                                                         {"data": "repaymentRef"},
                                                         {"data": "feeRepaymentAmt"},
                                                         {"data": "feeRepaymentRef"},
                                                         {"data": "feeBalanceAmt"},
                                                         {"data": "feeRepaymentStatus"},
                                                         {"data": "principalRepaymentAmt"},
                                                         {"data": "principalRepaymentRef"},
                                                         {"data": "principalBalanceAmt"},
//                                                         {"data": "principalRepaymentStatus"}
                                                     ]
                                        });
                                    table2.buttons().container().appendTo('#customerLoans .col-md-6:eq(0)');

                            },
                            error: function (e) {
                                $("#loading").hide();
                                console.log(e);
                            }
                        }); //end ajax
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
