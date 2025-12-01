function addClientAccount(reference, clientName) {
    $("#loading").show();
    $.ajax({
        url: "/newAccountForIBProfile",
        data: {reference: reference, clientName: clientName},
        type: 'POST',
        success: function (response) {
            $("#loading").hide();
            $("#myModal").modal("show");
            $("#areaValue").html(response);
        }
    });
}

function addSginatories(reference, clientName, categoryName) {
    $("#loading").show();
    $.ajax({
        url: "/checkAccounts",
        data: {reference: reference},
        type: 'POST',
        success: function (response) {
            $("#loading").hide();
            if (response.result === 0) {
                $.ajax({
                    url: "/addSignatoriesToProfile",
                    data: {reference: reference, clientName: clientName, categoryName: categoryName},
                    type: 'POST',
                    success: function (response) {
                        $("#loading").hide();
                        $("#myModal").modal("show");
                        $("#areaValue").html(response);
                    }
                });
            } else {
                swal("Error " + response.result, response.message, "error");
            }
        }
    });
}

//seen
function addMandate(reference, clientName, categoryName) {
    $("#loading").show();
    $.ajax({
        url: "/mandate",
        data: {reference: reference, clientName: clientName, categoryName: categoryName},
        type: 'POST',
        success: function (response) {
            $("#loading").hide();
            $("#myModal").modal("show");
            $("#areaValue").html(response);
        }
    });
}

function saveMandateToProfile(reference, clientName, categoryName) {
    $("#loading").show();
    $.ajax({
        url: "/mandate",
        data: {reference: reference, clientName: clientName, categoryName: categoryName},
        type: 'POST',
        success: function (response) {
            $("#loading").hide();
            $("#myModal").modal("show");
            $("#areaValue").html(response);
        }
    });
}

function addNewClientProfile() {
    $("#loading").show();
    var formdata = new FormData($('#createIbankProfile')[0]);
    $.ajax({
        url: "/createIBclientProfile",
        type: 'POST',
        data: formdata,
        enctype: 'multipart/form-data',
        processData: false,
        contentType: false,
        success: function (res) {
            $("#loading").hide();
            $("#note").hide();
            $('.error').html('');
            $('#profilesInitiated').DataTable().ajax.reload();
            if (res.validated) {
                $('#preloader2').hide();
                $('#rtgsTransferFormDiv').hide();
                $(function () {
                    setTimeout(function () {
                        showElement();
                    }, 3000);

                    function showElement() {
                        $("#successFlag").html('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"></button><strong>' + res.jsonString + '</strong></div>');
                    }
                });
            } else {
                $('#preloader2').hide();
                $("#successFlag").html('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + res.jsonString + '</strong></div>');
                $.each(res.errorMessages, function (key, value) {
                    $('Select[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');
                    $('input[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');
                    $('textarea[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');

                });
            }
        },
        error: function (res) {
            // $('.create-ib-client-profile').attr('disabled', 'disabled');
            $("#loading").hide();
            $('.error').html('');
            if (res.validated) {
                $.each(res.errorMessages, function (key, value) {
                    $('input[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');
                    $('Select[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');
                    $('textarea[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');
                });
            } else {
                if (typeof res.responseJSON.errors !== 'undefined') {
                    $.each(res.responseJSON.errors, function (key, value) {
                        $('input[name=' + value.field + ']').after('<span class="error" style="color:red">' + value.defaultMessage + '</span>');
                        $('Select[name=' + value.field + ']').after('<span class="error" style="color:red">' + value.defaultMessage + '</span>');
                        $('textarea[name=' + value.field + ']').after('<span class="error" style="color:red">' + value.defaultMessage + '</span>');

                    });
                } else {
                    $('#errorMessage').after('<div class="row"><h4 class="error" style="color:red"> Error: ' + res.responseJSON.message + '</h4></div>');
                }
            }
        }
    });
}

function addClientAccountToIBProfile() {
    $("#loading").show();
    var formdata = new FormData($('#newAccountToIBProfile')[0]);
    $.ajax({
        url: "/createIBclientProfileAccount",
        type: 'POST',
        data: formdata,
        enctype: 'multipart/form-data',
        processData: false,
        contentType: false,
        success: function (res) {
            $('#accountsIBProfile').DataTable().ajax.reload();
            $("#loading").hide();
            $('.error').html('');
            $('#profilesInitiated').DataTable().ajax.reload();
            if (res.validated) {
                $('#preloader2').hide();
                $('#rtgsTransferFormDiv').hide();
                $("#successFlag").html('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + res.jsonString + '</strong></div>');
            } else {
                $('#preloader2').hide();
                $("#successFlag").html('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + res.jsonString + '</strong></div>');
                $.each(res.errorMessages, function (key, value) {
                    $('Select[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');
                    $('input[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');
                    $('textarea[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');
                });
            }
        },
        error: function (res) {
            // $('.add-account-to-ib-profile').attr('disabled', 'disabled');
            $("#loading").hide();
            $('.error').html('');
            if (res.validated) {
                $.each(res.errorMessages, function (key, value) {
                    $('input[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');
                    $('Select[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');
                    $('textarea[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');

                });
            } else {
                if (typeof res.responseJSON.errors !== 'undefined') {
                    $.each(res.responseJSON.errors, function (key, value) {
                        $('input[name=' + value.field + ']').after('<span class="error" style="color:red">' + value.defaultMessage + '</span>');
                        $('Select[name=' + value.field + ']').after('<span class="error" style="color:red">' + value.defaultMessage + '</span>');
                        $('textarea[name=' + value.field + ']').after('<span class="error" style="color:red">' + value.defaultMessage + '</span>');

                    });
                } else {
                    $('#errorMessage').after('<div class="row"><h4 class="error" style="color:red"> Error: ' + res.responseJSON.message + '</h4></div>');
                }
            }
        }
    });

}

function addSignatoryToIBProfile() {
    $("#loading").show();
    var formdata = new FormData($('#addSignatoryToIBProfile')[0]);

    $.ajax({
        url: "/createSignatoryToIBProfile",
        type: 'POST',
        data: formdata,
        enctype: 'multipart/form-data',
        processData: false,
        contentType: false,
        success: function (res) {
            $("#loading").hide();
            $('.error').html('');
            $('#profilesInitiated').DataTable().ajax.reload();
            $('#accountsIBProfile').DataTable().ajax.reload();
            if (res.validated) {
                $('#preloader2').hide();
                $('#rtgsTransferFormDiv').hide();
                $("#successFlag").html('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert" aria-label="Close"> </button><strong>' + res.jsonString + '</strong></div>');
            } else {
                $('#preloader2').hide();
                $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert" aria-label="Close"> </button><strong>' + res.jsonString + '</strong></div>');
                $.each(res.errorMessages, function (key, value) {
                    $('Select[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');
                    $('input[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');
                    $('textarea[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');

                });
            }
        },
        error: function (res) {
            $('.create-ib-client-profile').attr('disabled', 'disabled');
            $("#loading").hide();
            $('.error').html('');
            if (res.validated) {
                $.each(res.errorMessages, function (key, value) {
                    $('input[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');
                    $('Select[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');
                    $('textarea[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');

                });
            } else {
                if (typeof res.responseJSON.errors !== 'undefined') {
                    $.each(res.responseJSON.errors, function (key, value) {
                        $('input[name=' + value.field + ']').after('<span class="error" style="color:red">' + value.defaultMessage + '</span>');
                        $('Select[name=' + value.field + ']').after('<span class="error" style="color:red">' + value.defaultMessage + '</span>');
                        $('textarea[name=' + value.field + ']').after('<span class="error" style="color:red">' + value.defaultMessage + '</span>');

                    });
                } else {
                    $('#errorMessage').after('<div class="row"><h4 class="error" style="color:red"> Error: ' + res.responseJSON.message + '</h4></div>');
                }
            }
        }
    });

}

// profileReference, clientName
function initiatorSubmitProfileForApproval(reference) {
    $("#loading").show();
    $.ajax({
        url: "/initiatorSubmitProfileForApproval",
        data: {reference: reference},
        type: 'POST',
        success: function (res) {
            console.log(res);
            $('#profilesInitiated').DataTable().ajax.reload();
            $("#loading").hide();
            if (res.validated) {
                $('#preloader2').hide();
                $('#listOfSignatories').hide();
                $('#myModal').modal('hide');
                $("#alertFlag").show();
                $("#alertFlag").html('<div class="alert alert-success" role="alert" id="arletFlag"><button type="button" class="close" data-dismiss="alert" aria-label="Close"><span aria-hidden="true">&times;</span></button><strong>' + res.jsonString + '</strong></div>');
            } else {
                $('#preloader2').hide();
                $("#alertFlag").html('<div class="alert alert-danger" role="alert" id="arletFlag"><button type="button" class="close" data-dismiss="alert" aria-label="Close"><span aria-hidden="true">&times;</span></button><strong>' + res.jsonString + '</strong></div>');
                $.each(res.errorMessages, function (key, value) {
                    $('Select[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');
                    $('input[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');
                    $('textarea[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');

                });
            }
        }
    });
}

function viewSignatoryAccess(signatoryId, profileReference) {
    $('#viewAccess').show();

    var token = $("meta[name='_csrf']").attr("content");
    var header = $("meta[name='_csrf_header']").attr("content");
    var table = $("#accountAccess").DataTable({
        dom: 'Blfrtip',
        lengthChange: true,
        scrollCollapse: true,
        scrollY: "700px",
        scrollX: true,
//        pageLength: 10,
//        lengthMenu: [
//            [5, 10, 25, 50, 100, 500, -1],
//            [5, 10, 25, 50, 100, 500, 'Show all']
//        ],
        responsive: false,
        processing: true,
        serverSide: true,
        serverMethod: 'post',
        order: [2, "desc"],
        ajax: {
            'url': '/getSignatoryAccountAccess',
            'beforeSend': function (request) {
                request.setRequestHeader(header, token);
            },
            "data": function (d) {
                return $.extend({}, d, {
                    "signatoryId": signatoryId,
                    "profile_reference": profileReference
                });
            }
        },
        "columns": [
//            {"data": "profile_reference"},
            {"data": "account_no"},
            {"data": "old_account_no"},
            {"data": "account_name"},
            {"data": "account_currency"},
            {"data": "account_prod_code"},
            {"data": "account_type"},
            {"data": "account_category"},
            {"data": "account_limit"},
            {"data": "signatoryLimit"},
            {
                "data": 'profile_reference',
                "orderable": false,
                "searchable": false,
                "render": function (data, type, row, meta) {
                    var a = '';
                    if (row.transfer_access == 'I') {
                        a = 'NOT ALLOWED';
                    }
                    if (row.transfer_access == 'A') {
                        a = 'ALLOWED';
                    }
                    return a;
                }
            },
            {
                "data": 'view_access',
                "orderable": false,
                "searchable": false,
                "render": function (data, type, row, meta) {
                    var a = '';
                    if (row.view_access == 'I') {
                        a = 'NOT ALLOWED';
                    }
                    if (row.view_access == 'A') {
                        a = 'ALLOWED';
                    }
                    return a;
                }
            }
        ], aoColumnDefs: [
            {
                bSortable: false,
                aTargets: [-1],
            }
        ], columnDefs: [
            {width: 1000, targets: 14}
        ],
//                fixedColumns: true,
        buttons: [],
        "preDrawCallback": function (settings) {
            if ($("select[name='mno']").val() == '') {
                return false;
            }
        }
    });
    var table2 = $("#rolesAccess").DataTable({
        dom: 'Blfrtip',
        lengthChange: false,
        scrollCollapse: true,
        scrollY: "700px",
        scrollX: true,
//        pageLength: 10,
//        lengthMenu: [
//            [5, 10, 25, 50, 100, 500, -1],
//            [5, 10, 25, 50, 100, 500, 'Show all']
//        ],
        responsive: false,
        processing: true,
        serverSide: true,
        serverMethod: 'post',
        order: [2, "desc"],
        ajax: {
            'url': '/getSignatoryRoleAccess',
            'beforeSend': function (request) {
                request.setRequestHeader(header, token);
            },
            "data": function (d) {
                return $.extend({}, d, {
                    "signatoryId": signatoryId,
                    "profile_reference": profileReference
                });
            }
        },
        "columns": [
//            {"data": "profile_reference"},
            {"data": "fullname"},
            {"data": "username"},
            {"data": "name"},
        ], aoColumnDefs: [
            {
                bSortable: false,
                aTargets: [-1],
            }
        ], columnDefs: [
            {width: 1000, targets: 14}
        ],
//                fixedColumns: true,
        buttons: [],
        "preDrawCallback": function (settings) {
            if ($("select[name='mno']").val() == '') {
                return false;
            }
        }
    });
    table.fnDraw();
    table2.fnDraw();
}

function viewPendingIBProfileOnBranchWorkFlow(reference, clientName, categoryName) {
    $("#loading").show();
    $.ajax({
        url: "/viewPendingIBProfileBranch",
        data: {reference: reference, clientName: clientName, categoryName: categoryName},
        type: 'POST',
        success: function (response) {
            $("#loading").hide();
            $("#myModal").modal("show");
            $("#areaValue").html(response);
        }
    });
}

function viewPendingIBProfileOnInitiatorWorkFlow(reference, clientName) {
    $("#loading").show();
    $.ajax({
        url: "/viewPendingIBProfileInitiator",
        data: {reference: reference, clientName: clientName},
        type: 'POST',
        success: function (response) {
            $("#loading").hide();
            $("#myModal").modal("show");
            $("#areaValue").html(response);
        }
    });
}

function viewPendingIBProfileAtHqWorkFlow(reference, clientName) {
    $("#loading").show();
    $.ajax({
        url: "/viewPendingIBProfileHq",
        data: {reference: reference, clientName: clientName},
        type: 'POST',
        success: function (response) {
            $("#loading").hide();
            $("#myModal").modal("show");
            $("#areaValue").html(response);
        }
    });
}


function approveIBProfileAtBranchLevel(reference) {
    // $('#authorize').attr('disabled', 'disabled');
    // $('#cancel').attr('disabled', 'disabled');
    // $('#returnForamendment').attr('disabled', 'disabled');
    $("#loading").show();
    $.ajax({
        url: "/approveIBProfileAtBranchLevel",
        data: {reference: reference},
        type: 'POST',
        success: function (response) {
            $("#loading").hide();
            $("#messageData").hide();
            $('#authorize').hide();
            $('#cancel').hide();
            $('#return').hide();
            if (response.result === 1) {
                $("#successFlag").after('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + '</strong></div>');
            } else {
                $("#successFlag").after('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + response.result + '</strong></div>');
            }
            $('#successFlag').delay(2000).show();
            reloadDatatable("#profilesInitiated");
        }
    });

}

function approveIBProfileAtHqLevel(reference) {
    $("#loading").show();
    $.ajax({
        url: "/registerIbProfile",
        data: {reference: reference},
        type: 'POST',
        success: function (response) {
            // console.log(response);
            $("#loading").hide();
            $("#messageData").hide();
            $('#authorize').hide();
            $('#cancel').hide();
            $('#return').hide();
            if (response.result === 0) {
                $("#successFlag").after('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + '</strong></div>');
            } else {
                $("#successFlag").after('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"asuccessFlagria-label="Close"> </button><strong>' + response.message + response.result + '</strong></div>');
            }
            $('#successFlag').delay(2000).show();
            reloadDatatable("#profilesInitiated");
        }
    });


}

function removeAccfromProfile(reference, accountNumber) {
    $("#loading").show();
    $.ajax({
        url: "/removeAccountFromIbProfile",
        data: {reference: reference, accountNumber: accountNumber},
        type: 'POST',
        success: function (response) {
            $("#loading").hide();
            $('#accountsIBProfile').DataTable().ajax.reload();
            $('.error').html('');
            $('#profilesInitiated').DataTable().ajax.reload();
            if (response.validated) {
                $('#preloader2').hide();
                $('#rtgsTransferFormDiv').hide();
                $("#successFlag").html('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"aria-label="Close"> </button><strong>' + response.message + '</strong></div>');
            } else {
                $('#preloader2').hide();
                $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"aria-label="Close"> </button><strong>' + response.message + '</strong></div>');
                $.each(response.message, function (key, value) {
                    $('Select[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');
                    $('input[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');
                    $('textarea[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');

                })
            }
        }
    });

}

function removeSignatoryFromProfile(reference, rimNo) {
    $("#loading").show();
    $.ajax({
        url: "/removeSignatoryFromIbProfile",
        data: {reference: reference, rimNumber: rimNo},
        type: 'POST',
        success: function (response) {
            $("#loading").hide();
            $('#accountsIBProfile').DataTable().ajax.reload();
            $('.error').html('');
            $('#profilesInitiated').DataTable().ajax.reload();
            if (response.validated) {
                $('#preloader2').hide();
                $('#rtgsTransferFormDiv').hide();
                $("#successFlag").html('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"aria-label="Close"> </button><strong>' + response.message + '</strong></div>');
            } else {
                $('#preloader2').hide();
                $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"aria-label="Close"> </button><strong>' + response.message + '</strong></div>');
                $.each(response.message, function (key, value) {
                    $('Select[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');
                    $('input[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');
                    $('textarea[name=' + key + ']').after('<span class="error" style="color:red">' + value + '</span>');

                })
            }
        }
    });

}

function changeUserPasswordAjax(custId,userName,fullName,partnerCode) {
    $("#loading").show();
    $.ajax({
        url: "/firechangeUserPasswordModalAjax",
        data: {custId: custId, userName: userName, fullName: fullName, partnerCode: partnerCode},
        type: 'POST',
        success: function (response) {
            $("#loading").hide();
            $("#myModal").modal("show");
            $("#areaValue").html(response);
        }
    });
}



function resetPasswordIBUser(userName, custId,partnerCode) {
    $("#resetIBUser").attr('disabled','disabled');
    $.ajax({
        url: "/fireResetPasswordIBUserAjax",
        data: { userName: userName,custId: custId, partnerCode: partnerCode},
        type: 'POST',
        success: function (response) {
             console.log(response.responseCode);
            if (response.responseCode == 0) {
                iziToast.success({ title: 'SUCCESS', message: 'Password changed', color:'green',position:'topRight', timeout: 7000 });
            } else {
                iziToast.error({ title: 'FAIL', message: 'Failed To Change Password', color:'red',position:'topRight', timeout: 7000 });
            }
        }
    });
}

function modifyUserIBDetails(custId,userName,id,partnerCode) {
    $("#loading").show();
    $.ajax({
        url: "/fireModifyUserIBDetailsModalAjax",
        data: {custId: custId, userName: userName, id: id, partnerCode: partnerCode},
        type: 'POST',
        success: function (response) {
            $("#loading").hide();
            $("#myModal").modal("show");
            $("#areaValue").html(response);
        }
    });
}

function updateIbUsersDetailsChange() {
    $("#updateIBUser").attr('disabled','disabled');
    var token = $("meta[name='_csrf']").attr("content");
    var header = $("meta[name='_csrf_header']").attr("content");
    var formdata = new FormData($('#ibUsersDetailsChange')[0]);
    console.log(formdata);
    $.ajax({
        url: "/fireUpdateIBUsersDetailsAjax",
        data: formdata,
        type: 'POST',
        processData: false,
        contentType: false,
        success: function (response) {
             console.log(response.responseCode);
            if (response.responseCode == 0) {
                iziToast.success({ title: 'SUCCESS', message: 'User Details Updated Successfully', color:'green',position:'topRight', timeout: 4000 });
            } else {
                iziToast.error({ title: 'FAIL', message: 'Failed To Update User Details', color:'red',position:'topRight', timeout: 3000 });
            }
        }
    });


}

