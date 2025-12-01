let acctProducts = [];

function getCustomerKycAndAccountDetails(trackingNo, branchNo, category, products) {
    $("#loading").show();
    acctProducts = products;
    $.ajax({
        url: "/getCustomerKycAndAccountDetails",
        data: {trackingNo: trackingNo, branchNo: branchNo, category: category},
        type: 'GET',
        success: function (response) {
            $("#loading").hide();
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
            htmlData += '<div class="panel panel-default panel-fill" id="customer_kyc"><div class="panel-heading no-print"><h4 class="panel-title">KYC INFORMATION</h4>';
            if (!response.hasRim)
                htmlData += '<a style="background-color: #535852 !important; border: 1px solid #0ab01e  !important; !important;margin-top: -22px !important;" class="btn btn-primary pull-right" href="javascript:void(0)" onclick="createRim('+trackingNo+',\''+category+'\'); return false;" id="create_rim">Create Rim</a>';
            if (!response.hasAccount && response.hasRim)
                htmlData += '<a style="background-color: #0a3f75 !important;border: 1px solid #0ab01e  !important;margin-top: -22px !important;" class="btn btn-primary pull-right" href="javascript:void(0)" onclick="createAccount('+trackingNo+'); return false;" id="create_account">Create Acct</a>';
            htmlData += '</div>';
            htmlData += '<div class="panel-body">';
            htmlData += '<div class="col-md-12 pdf-content"></div>';
            htmlData += '</div>';
            htmlData += '</div>';
            htmlData += '</div>';
            $("#areaValue").html(htmlData);
            $(".pdf-content").append(obj);
        }
    });
}

function getAccountDetails(customerNo, accountNo, branchNo, category) {
   $("#loading").show();
   $.ajax({
       url: "/getAccountDetails",
       data: {customerNo: customerNo, accountNo: accountNo, branchNo: branchNo, category: category},
       type: 'GET',
       success: function (response) {
           $("#loading").hide();
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
           htmlData += '<div class="panel panel-default panel-fill" id="customer_kyc"><div class="panel-heading no-print"><h4 class="panel-title">KYC INFORMATION</h4>';
           if (!response.isEnrolled && response.hasAccount)
               htmlData += '<a style="background-color: #750a0a !important; border: 1px solid #0ab01e  !important;margin-top: -22px !important;" class="btn btn-primary pull-right" href="javascript:void(0)" onclick="attachMobileBanking('+accountNo+'); return false;" id="attach_mobile_service">Attach Mobile</a>';
           htmlData += '</div>';
           htmlData += '<div class="panel-body">';
           htmlData += '<div class="col-md-12 pdf-content"></div>';
           htmlData += '</div>';
           htmlData += '</div>';
           htmlData += '</div>';
           $("#areaValue").html(htmlData);
           $(".pdf-content").append(obj);
       }
   });
}

function viewCustomerAttachments(trackingNo) {
    $("#loading").show();
    $.ajax({
        url: "/viewCustomerAttachment",
        data: {trackingNo: trackingNo},
        type: 'GET',
        success: function (response) {
            $("#loading").hide();
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
            htmlData += '<div class="panel panel-default panel-fill" id="customer_kyc">';
            htmlData += '<div class="panel-heading no-print"><h4 class="panel-title">CUSTOMER PHOTO</h4></div>';
            htmlData += '<div class="panel-body">';
            htmlData += '<div class="col-md-12 pdf-content"></div>';
            htmlData += '</div>';
            htmlData += '</div>';
            htmlData += '</div>';
            $("#areaValue").html(htmlData);
            $(".pdf-content").append(obj);
        }
    });
}

function viewAccountAttachments(customerNo) {
    $("#loading").show();
    $.ajax({
        url: "/viewAccountAttachment",
        data: {customerNo: customerNo},
        type: 'GET',
        success: function (response) {
            $("#loading").hide();
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
            htmlData += '<div class="panel panel-default panel-fill" id="customer_kyc">';
            htmlData += '<div class="panel-heading no-print"><h4 class="panel-title">CUSTOMER PHOTO</h4></div>';
            htmlData += '<div class="panel-body">';
            htmlData += '<div class="col-md-12 pdf-content"></div>';
            htmlData += '</div>';
            htmlData += '</div>';
            htmlData += '</div>';
            $("#areaValue").html(htmlData);
            $(".pdf-content").append(obj);
        }
    });
}

function editCustomerDetails(trackingNo) {
    $("#loading").show();
    $.ajax({
        url: "/getKYCCustomerDetails",
        data: {trackingNo: trackingNo},
        type: 'GET',
        success: function (response) {
            $("#loading").hide();
            $("#myModal").modal("show");
            var displayDate = response.dob.split('T')[0];
            var htmlData = '<div class="row">';
            htmlData += '<div class="panel panel-default panel-fill">';
            htmlData += '<div class="panel-heading"><h4 class="panel-title">EDIT CUSTOMER</h4></div>';
            htmlData += '<div class="panel-body">';
            htmlData += '<div class="col-md-12">';
            htmlData += '<form class="form-horizontal" role="form" id="edit_customer" action="" method="POST">';
            htmlData += '<fieldset>';
            htmlData += '<legend>Name:</legend>';
            htmlData += '<div class="row">';
            htmlData += '<div class="col-md-3">';
            htmlData += '<label>First Name</label>';
            htmlData += '<input type="text" id="first_name" name="first_name" value="' + response.first_name + '"';
            htmlData += ' class="form-control form-control-sm" autocomplete="off"';
            htmlData += ' style="font-size: 10px !important;" placeholder="First Name">';
            htmlData += '</div>';
            htmlData += '<div class="col-md-3">';
            htmlData += '<label>Middle Name</label>';
            htmlData += '<input type="text" id="middle_name" name="middle_name" value="' + response.middle_name + '"';
            htmlData += ' class="form-control form-control-sm" autocomplete="off"';
            htmlData += ' style="font-size: 10px !important;" placeholder="Middle Name">';
            htmlData += '</div>';
            htmlData += '<div class="col-md-3">';
            htmlData += '<label>Last Name</label>';
            htmlData += '<input type="text" id="last_name" name="last_name" value="' + response.last_name + '"';
            htmlData += ' class="form-control form-control-sm" autocomplete="off"';
            htmlData += ' style="font-size: 10px !important;" placeholder="Last Name">';
            htmlData += '</div>';
            htmlData += '</div>';
            htmlData += '</fieldset>';
            htmlData += '<fieldset>';
            htmlData += '<legend>Bio:</legend>';
            htmlData += '<div class="row">';
            htmlData += '<div class="col-md-2">';
            htmlData += '<label>Date of Birth</label>';
            htmlData += '<input type="date" id="dob" name="dob" value="' + displayDate + '"';
            htmlData += ' class="form-control form-control-sm" autocomplete="off"';
            htmlData += ' style="font-size: 10px !important;">';
            htmlData += '</div>';
            htmlData += '<div class="col-md-1">';
            htmlData += '<label>Gender</label>';
            htmlData += '<select class="form-control form-control-sm col-sm-12 select2" id="gender" name="gender"';
            htmlData += ' style="font-size: 10px !important;">';
            htmlData += '<option value="Male">M</option>';
            htmlData += '<option value="Female">F</option>';
            htmlData += '</select>';
            htmlData += '</div>';
            htmlData += '<div class="col-md-3">';
            htmlData += '<label>Title</label>';
            htmlData += '<select class="form-control form-control-sm col-sm-12 select2" id="title" name="title"';
            htmlData += ' style="font-size: 10px !important;">';
            htmlData += '<option value="Mr">Mr</option>';
            htmlData += '<option value="Mrs">Mrs</option>';
            htmlData += '<option value="Ms">Ms</option>';
            htmlData += '<option value="Miss">Miss</option>';
            htmlData += '<option value="Dr">Dr</option>';
            htmlData += '<option value="Professor">Professor</option>';
            htmlData += '<option value="Engineer">Engineer</option>';
            htmlData += '<option value="Sir">Sir</option>';
            htmlData += '<option value="Rev">Rev</option>';
            htmlData += '<option value="Alhaji">Alhaji</option>';
            htmlData += '<option value="Apostle">Apostle</option>';
            htmlData += '<option value="Justice">Justice</option>';
            htmlData += '<option value="Ndugu">Ndugu</option>';
            htmlData += '<option value="Honourable">Honourable</option>';
            htmlData += '<option value="Ambassador">Ambassador</option>';
            htmlData += '<option value="Mheshimiwa">Mheshimiwa</option>';
            htmlData += '<option value="Hajjat">Hajjat</option>';
            htmlData += '<option value="Father">Father</option>';
            htmlData += '<option value="Bishop">Bishop</option>';
            htmlData += '<option value="Monsr">Monsr</option>';
            htmlData += '<option value="Other">Other</option>';
            htmlData += '</select>';
            htmlData += '</div>';
            htmlData += '<div class="col-md-3">';
            htmlData += '<label>Marital Status</label>';
            htmlData += '<select class="form-control form-control-sm col-sm-12 select2" id="maritalStatus" name="marital_status"';
            htmlData += ' autocomplete="off" style="font-size: 10px !important;" placeholder="Marital Status">';
            htmlData += '<option value="Single">Single</option>';
            htmlData += '<option value="Married">Married</option>';
            htmlData += '<option value="Divorced">Divorced</option>';
            htmlData += '<option value="Widowed">Widowed</option>';
            htmlData += '</select>';
            htmlData += '</div>';
            htmlData += '</div>';
            htmlData += '</fieldset>';
            htmlData += '<fieldset>';
            htmlData += '<legend>Contact:</legend>';
            htmlData += '<div class="row">';
            htmlData += '<div class="col-md-3">';
            htmlData += '<label>Email</label>';
            htmlData += '<input type="email" id="email" name="email_address" class="form-control form-control-sm" value="' + response.email_address + '"';
            htmlData += ' autocomplete="off" style="font-size: 10px !important;" placeholder="Email Address">';
            htmlData += '</div>';
            htmlData += '<div class="col-md-3">';
            htmlData += '<label>Mobile Number</label>';
            htmlData += '<input type="tel" id="phoneNo" name="phone_number" value="' + response.phone_number + '"';
            htmlData += ' class="form-control form-control-sm" autocomplete="off"';
            htmlData += ' style="font-size: 10px !important;">';
            htmlData += '</div>';
            htmlData += '<div class="col-md-3">';
            htmlData += '<label>Other Phone Number</label>';
            htmlData += '<input type="tel" id="otherPhoneNo" name="other_phone_number" value="' + response.other_phone_number + '"';
            htmlData += ' class="form-control form-control-sm" autocomplete="off"';
            htmlData += ' style="font-size: 10px !important;">';
            htmlData += '</div>';
            htmlData += '</div>';
            htmlData += '</fieldset>';
            htmlData += '<fieldset>';
            htmlData += '<legend>Residence:</legend>';
            htmlData += '<div class="row">';
            htmlData += '<div class="col-md-3">';
            htmlData += '<label>Region</label>';
            htmlData += '<select class="form-control form-control-sm col-sm-12 select2" id="region" name="residence_region"';
            htmlData += ' autocomplete="off" style="font-size: 10px !important;" placeholder="Region"/>';
            htmlData += '</div>';
            htmlData += '<div class="col-md-3">';
            htmlData += '<label>District</label>';
            htmlData += '<select class="form-control form-control-sm col-sm-12 select2" id="district" name="residence_district"';
            htmlData += ' autocomplete="off" style="font-size: 10px !important;" placeholder="District">';
            htmlData += '<option value="' + response.residence_district + '">' + response.residence_district + '</option>';
            htmlData += '</select>';
            htmlData += '</div>';
            htmlData += '<div class="col-md-3">';
            htmlData += '<label>Area</label>';
            htmlData += '<input type="text" id="area" name="residence_area" class="form-control form-control-sm" value="' + response.residence_area + '"';
            htmlData += ' autocomplete="off" style="font-size: 10px !important;" placeholder="Area">';
            htmlData += '</div>';
            htmlData += '<div class="col-md-3">';
            htmlData += '<label>House No</label>';
            htmlData += '<input type="text" id="houseNo" name="residence_house_no" class="form-control form-control-sm" value="' + response.residence_house_no + '"';
            htmlData += ' autocomplete="off" style="font-size: 10px !important;" placeholder="House No">';
            htmlData += '</div>';
            htmlData += '</div>';
            htmlData += '</fieldset>';
            htmlData += '<fieldset>';
            htmlData += '<legend>Identification:</legend>';
            htmlData += '<div class="row">';
//            htmlData += '<div class="col-md-3">';
//            htmlData += '<label>Photo</label>';
//            htmlData += '<input type="file" id="photo" name="photo" class="form-control form-control-sm"';
//            htmlData += ' autocomplete="off" style="font-size: 10px !important;">';
//            htmlData += '</div>';
//            htmlData += '<div class="col-md-3">';
//            htmlData += '<label>Signature</label>';
//            htmlData += '<input type="file" id="photo" name="photo" class="form-control form-control-sm"';
//            htmlData += ' autocomplete="off" style="font-size: 10px !important;">';
//            htmlData += '</div>';
//            htmlData += '<div class="col-md-3">';
//            htmlData += '<label>Attachment</label>';
//            htmlData += '<input type="file" id="attachment" name="attachment" class="form-control form-control-sm"';
//            htmlData += ' autocomplete="off" style="font-size: 10px !important;">';
//            htmlData += '</div>';
            htmlData += '<div class="col-md-3">';
            htmlData += '<label>ID Number</label>';
            htmlData += '<input type="text" id="idNumber" name="id_number" class="form-control form-control-sm" value="' + response.id_number + '"';
            htmlData += ' autocomplete="off" style="font-size: 10px !important;" placeholder="ID Number">';
            htmlData += '</div>';
            htmlData += '</div>';
            htmlData += '</fieldset>';
            htmlData += '<fieldset>';
            htmlData += '<legend>Other:</legend>';
            htmlData += '<div class="row">';
            htmlData += '<div class="col-md-3">';
            htmlData += '<label>City</label>';
            htmlData += '<input type="text" id="city" name="city" class="form-control form-control-sm" value="' + response.city + '"';
            htmlData += ' autocomplete="off" style="font-size: 10px !important;" placeholder="City">';
            htmlData += '</div>';
            htmlData += '<div class="col-md-3">';
            htmlData += '<label>Product</label>';
            htmlData += '<select class="form-control form-control-sm col-sm-12 select2" id="product" name="product_id"';
            htmlData += ' autocomplete="off" style="font-size: 10px !important;" placeholder="Product">';
            htmlData += '<option value="76">Quick Account</option>';
            htmlData += '<option value="80">Uni-card Account</option>';
            htmlData += '<option value="64">VSLA Account</option>';
            htmlData += '<option value="202">Platinum Account</option>';
            htmlData += '<option value="383">Salary Quick Account</option>';
            htmlData += '</select>';
            htmlData += '</div>';
            htmlData += '<div class="col-md-3">';
            htmlData += '<label>Type</label>';
            htmlData += '<select class="form-control form-control-sm col-sm-12 select2" id="type" name="customer_type_id"';
            htmlData += ' autocomplete="off" style="font-size: 10px !important;" placeholder="Type">';
            htmlData += '<option value="718950">Remote Individual</option>';
            htmlData += '</select>';
            htmlData += '</div>';
            htmlData += '</div>';
            htmlData += '</fieldset>';
            htmlData += '<div class="row text-center">';
            htmlData += '<button type="button" class="btn btn-primary edit-customer-profile" onclick="editCustomerProfile(' + trackingNo + ')">';
            htmlData += 'Save Customer Profile';
            htmlData += '</button>';
            htmlData += '</div>';
            htmlData += '</form>';
            htmlData += '</div>';
            htmlData += '</div>';
            htmlData += '</div>';
            htmlData += '</div>';
            $("#areaValue").html(htmlData);
            $.getJSON("assets/upload/region-district.json", function(data) {
                let count = Object.keys(data).length;
                for (var i = 0; i < count; i++) {
                    let region = Object.keys(data)[i];
                    $("#region").append('<option value="'+region+'">'+region+'</option>');
                }
                $("#region").val(response.residence_region);
            }).fail(function(){
                console.log("An error has occurred.");
            });
            $('#region').change(function(){
                $.getJSON("assets/upload/region-district.json", function(data) {
                    let count = Object.keys(data).length;
                    var selectedVal = $("#region option:selected").val();
                    for (var i = 0; i < count; i++) {
                        let region = Object.keys(data)[i];
                        if (region == selectedVal) {
                            $.each(data[Object.keys(data)[i]], function(i, district) {
                                $('<option value="'+district+'">'+district+'</option>').appendTo('#district');
                            });
                        }
                    }
                });
                $('#district').empty();
            });
            $("#gender").val(response.gender);
            $("#title").val(response.title);
            $("#maritalStatus").val(response.marital_status);
            $("#district").val(response.residence_district);
            $("#type").val(response.customer_type_id);
            $("#product").val(response.product_id);
        }
    });
}

function editCustomerProfile(trackingNo) {
    var formData = new FormData($('#edit_customer')[0]);
    formData.set('dob', formData.get('dob') + 'T03:00:00');
    $("#loading").show();
    $.ajax({
        url: "/editCustomerDetails",
        data: {trackingNo: trackingNo, form: JSON.stringify(Object.fromEntries(formData))},
        type: 'POST',
        success: function (response) {
            $("#loading").hide();
            if (response.responseCode == "0") {
                swal("Success!", response.message, "success");
                $('#agentsKyc').DataTable().ajax.reload();
            } else {
                swal("Error!", response.message, "error");
            }
        }
    });
}

function createRim(userId, category) {
    $("#loading").show();
    $.ajax({
        url: "/createRim",
        data: {userId: userId, category: category},
        type: 'POST',
        success: function (response) {
            $("#loading").hide();
            $("#myModal").modal("show");
            var htmlData = '<div class="row">';
            htmlData += '<div class="panel panel-default">';
            if (response.success) {
                htmlData += '<div class="panel-heading"><h4>Success</h4></div>';
                htmlData += '<div class="panel-body">';
                htmlData += '<div class="col-md-6"><h4>RIM created successfully!</h4><br/>';
                htmlData += '<h6>RIM number is <b><strong>' + response.rim + '</strong></b></h6><br/>';
                htmlData += '<p>Reference number: <b>' + response.reference + '</b></p></div>';
            } else {
                htmlData += '<div class="panel-heading"><h4>Failed</h4></div>';
                htmlData += '<div class="panel-body">';
                htmlData += '<div class="col-md-6"><h5>RIM creation failed!</h5></div>';
            }
            htmlData += '</div>';
            htmlData += '</div>';
            htmlData += '</div>';
            $("#areaValue").html(htmlData);
        }
    });
}

function createAccount(userId) {
    var options = {};
    $.map(acctProducts,
        function(product) {
            options[product.product_code] = product.product_name;
        });
    swal({
        title: 'Choose account product',
        input: 'select',
        inputOptions: options,
        inputPlaceholder: 'Choose account product',
        type: "question",
        showCancelButton: true,
        confirmButtonText: 'Submit',
        confirmButtonColor: "#3ba03e"
    })
    .then((result) => {
        if (result) {
            swal.close();
            $("#loading").show();
            $.ajax({
                url: "/createAccount",
                data: {userId: userId, productId: result},
                type: 'POST',
                success: function (response) {
                    $("#loading").hide();
                    if (response.success) {
                        swal("Account created successfully!", "Reference number: " + response.reference + "\nAccount number for RIM " + response.rim + " is " + response.account, "success");
                    } else {
                        swal("Error!", "Account creation failed!", "error");
                    }
                }
            });
        }
    });
}

function attachMobileBanking(acctNo) {
    $("#loading").show();
    $.ajax({
        url: "/enrollMobileBanking",
        data: {acctNo: acctNo},
        type: 'POST',
        success: function (response) {
            $("#loading").hide();
            if (response.success) {
                swal("Success!", "Enrollment successful!", "success");
            } else {
                swal("Error!", "Enrollment failed!", "error");
            }
        }
    });
}

function enableAgent(userId, branchNo) {
    $("#loading").show();
    $.ajax({
        url: "/enableAgent",
        data: {userId: userId, branchNo: branchNo},
        type: 'POST',
        success: function (response) {
            $("#loading").hide();
            $("#myModal").modal("show");
            if (response.success) {
                swal("Success!", "Agent enabled successfully!", "success");
                $('#agentsKyc').DataTable().ajax.reload();
            } else {
                swal("Error!", "Failed to enable agent!", "error");
            }
        }
    });
}

function disableAgent(userId, branchNo) {
    $("#loading").show();
    $.ajax({
        url: "/disableAgent",
        data: {userId: userId, branchNo: branchNo},
        type: 'POST',
        success: function (response) {
            $("#loading").hide();
            $("#myModal").modal("show");
            if (response.success) {
                swal("Success!", "Agent disabled successfully!", "success");
                $('#agentsKyc').DataTable().ajax.reload();
            } else {
                swal("Error!", "Failed to disable agent!", "error");
            }
        }
    });
}

function getGroupKycAndAccountDetails(trackingNo, branchNo) {
    $("#loading").show();
    $.ajax({
        url: "/getGroupKycAndAccountDetails",
        data: {trackingNo: trackingNo, branchNo: branchNo},
        type: 'GET',
        success: function (response) {
            $("#loading").hide();
            $("#myModal").modal("show");
            var htmlData = '<div class="row">';
            htmlData += '<div class="panel panel-default panel-fill" id="group_kyc"><div class="panel-heading"> <h4 class="panel-title">GROUP KYC INFORMATION</h4>';
            htmlData += '<a style="background-color: #22c418 !important;border: 1px solid #0ab01e  !important;margin-top: -22px !important;" class="btn btn-primary pull-right" href="javascript:window.print()" id="print">Print</a>';
            htmlData += '<a style="background-color: #535852  !important; border: 1px solid #0ab01e  !important; !important;margin-top: -22px !important;" class="btn btn-primary pull-right" href="#" id="createGroupRim">Create Rim</a>';
            htmlData += '<a style="background-color: #0a3f75 !important;border: 1px solid #0ab01e  !important;margin-top: -22px !important;" class="btn btn-primary pull-right" href="#" id="createGroupAccount">Create Acct</a>';
            htmlData += '<a style="background-color: #750a0a !important; border: 1px solid #0ab01e  !important;margin-top: -22px !important;" class="btn btn-primary pull-right" href="#" id="attachGroupMobileService">Attach Mobile</a>';
            htmlData += '</div>';
            htmlData += '<div class="panel-body">';
            htmlData += '<div class="col-md-6">' + response + '</div>';
            htmlData += '</div>';
            htmlData += '</div>';
            htmlData += '</div>';
            $("#areaValue").html(htmlData);
        }
    });
}

function getGroupSignatories(trackingNo) {
    $("#loading").show();
    $.ajax({
        url: "/groups",
        data: {trackingNo: trackingNo},
        type: 'GET',
        success: function (response) {
            $("#loading").hide();
            $("#myModal").modal("show");
            var htmlData = '<div class="row">';
            htmlData += '<div class="panel panel-default panel-fill" id="group_signatories"><div class="panel-heading"> <h4 class="panel-title">GROUP/BUSINESS SIGNATORIES</h4>';
            htmlData += '<a style="background-color: #22c418 !important;border: 1px solid #0ab01e  !important;margin-top: -22px !important;" class="btn btn-primary pull-right" href="javascript:window.print()" id="print">Print</a>';
            htmlData += '</div>';
            htmlData += '<div class="panel-body">';
            htmlData += '<table id="signatories" class="display nowrap table table-striped table-bordered" style="width: 100%; font-size: 9.5px !important;">';
            htmlData += '<thead>';
            htmlData += '<tr>';
            htmlData += '<th>S/N</th>';
            htmlData += '<th>Tracking No</th>';
            htmlData += '<th>Rim no</th>';
            htmlData += '<th>First name</th>';
            htmlData += '<th>Middle name</th>';
            htmlData += '<th>Last Name</th>';
            htmlData += '<th>Phone</th>';
            htmlData += '<th>Identity No</th>';
            htmlData += '<th>Status</th>';
            htmlData += '<th>Rubikon Status</th>';
            htmlData += '</tr>';
            htmlData += '</thead>';
            htmlData += '<tbody>';
            var index = 0;
            var signatory = '';
            for (index = 0; index < response.aaData.length; index++) {
                signatory = response.aaData[index];
                var trackingNo = '';
                var rimNo = '';
                if (signatory.cust_no.startsWith('0000')) {
                    rimNo = signatory.cust_no;
                } else {
                    trackingNo = signatory.cust_no;
                }
                htmlData += '<tr>';
                htmlData += '<td>' + signatory.id + '</td>';
                htmlData += '<td>' + trackingNo + '</td>';
                htmlData += '<td>' + rimNo + '</td>';
                htmlData += '<td>' + signatory.first_name + '</td>';
                htmlData += '<td>' + signatory.middle_name + '</td>';
                htmlData += '<td>' + signatory.last_name + '</td>';
                htmlData += '<td>' + signatory.phone_number + '</td>';
                htmlData += '<td>' + signatory.id_number + '</td>';
                htmlData += '<td>' + signatory.action_status + '</td>';
                htmlData += '<td>' + signatory.cbs_response_code + '</td>';
                htmlData += '</tr>';
            }
            htmlData += '</tbody>';
            htmlData += '</table>';
            htmlData += '</div>';
            htmlData += '</div>'
            $("#areaValue").html(htmlData);
        }
    });
}

function updateAgentDevice(id) {
    $("#loading").show();
    $.ajax({
        url: "/updateAgentDevice",
        data: {id: id},
        type: 'POST',
        success: function (response) {
            $("#loading").hide();
            if (response.success) {
                swal("Success!", response.message, "success");
                $('#agentsKyc').DataTable().ajax.reload();
            } else {
                swal("Error!", response.message, "error");
            }
        }
    });
}

function updateAgentBranch(id, branches) {
    var options = {};
    $.map(branches,
        function(branch) {
            options[branch.code] = branch.name;
        });
    swal({
        title: "Change branch",
        input: 'select',
        inputOptions: options,
        inputPlaceholder: 'Select branch',
        type: "question",
        showCancelButton: true,
        confirmButtonText: 'Submit',
        confirmButtonColor: "#3ba03e"
    })
    .then((result) => {
        if (result) {
            swal.close();
            $("#loading").show();
            $.ajax({
                url: "/api/updateAgentBranch",
                data: {id: id, newBranch: result},
                type: 'POST',
                success: function (response) {
                    $("#loading").hide();
                    if (response.success) {
                        swal("Success!", response.message, "success");
                        $('#agentsKyc').DataTable().ajax.reload();
                    } else {
                        swal("Error!", response.message, "error");
                    }
                }
            });
        }
    });
}

function previewAccounts(id, messageId, noOfTxn, userRoles, messageType) {
    $("#loading").show();
    var htmlData = '<div class="row"><div class="panel panel-default"><div class="panel-header">';
    htmlData += '<h3 style="text-align: center;"><strong>('+noOfTxn+')</strong> ACCOUNTS FOR BATCH: '+messageId+'</h3></div>';
    htmlData += '<div class="panel-body"><div id="modal-alert"></div>';
    htmlData += '<div class="row"><div class="col-md-12">';
    htmlData += '<table id="batchAccounts" class="display nowrap table table-striped table-bordered" style="width: 100%; font-size: 9.5px !important;">';
    htmlData += '<thead>';
    htmlData += '<tr>';
    htmlData += '<th>S/N</th>';
    htmlData += '<th>End To End ID</th>';
    htmlData += '<th>Account No</th>';
    htmlData += '<th>Account Title</th>';
    htmlData += '<th>Currency</th>';
    htmlData += '<th>Category</th>';
    htmlData += '<th>Account Type</th>';
    htmlData += '<th>Branch</th>';
    htmlData += '<th>Operator</th>';
    htmlData += '<th>Operator Category</th>';
    htmlData += '<th>Purpose</th>';
    htmlData += '<th>Owner</th>';
    htmlData += '<th>Status</th>';
    htmlData += '<th>Status Description</th>';
    htmlData += '<th>Trans BIC</th>';
    htmlData += '<th>Trans Account no</th>';
    htmlData += '<th>Trans Account name</th>';
    htmlData += '<th>Trans Currency</th>';
    htmlData += '<th>Region</th>';
    htmlData += '<th>District</th>';
    htmlData += '<th>Phone No</th>';
    htmlData += '<th>Email</th>';
    htmlData += '<th>Postal Address</th>';
    htmlData += '<th>Actions</th>';
    htmlData += '</tr>';
    htmlData += '</thead>';
    htmlData += '<tbody></tbody>';
    htmlData += '</table>';
    htmlData += '</div></div></div></div></div>';
    $("#loading").hide();
    $("#myModal").modal("show");
    $("#areaValue").html(htmlData);
    var table = $('#batchAccounts').DataTable({
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
            'url': '/previewBatchAccountsList',
            'data': function (d) {
                return $.extend({}, d, {
                    "workflowId": id
                });
            }
        },
        "columns": [
            {"data": null},
            {"data": "endtoEndId"},
            {"data": "account_no"},
            {"data": "account_title"},
            {"data": "ccy"},
            {"data": "category"},
            {"data": "acct_type"},
            {"data": "branch"},
            {"data": "operator"},
            {"data": "operatorCat"},
            {"data": "purpose"},
            {"data": "owner"},
            {"data": "status"},
            {"data": "status_desc"},
            {"data": "transBic"},
            {"data": "transAcctNum"},
            {"data": "transAcctName"},
            {"data": "transCcy"},
            {"data": "regionCode"},
            {"data": "district"},
            {"data": "phoneNum"},
            {"data": "email"},
            {"data": "postalAddr"},
            {
                "data": "endtoEndId",
                "render": function (data, type, row, meta) {
                    var a = '';
                    if ((userRoles.includes('21') || userRoles.includes('48')) && row.status == 'Pending' && messageType == 'AccountOpening') {
                        a += '&nbsp;<button type="button" class="btn btn-success" onclick="acceptAccount(\'' + row.id + '\',\'' + noOfTxn + '\')">Accept</button>';
                    } else if ((userRoles.includes('21') || userRoles.includes('26')) && row.status == 'Awaiting Maker' && messageType == 'AccountOpening') {
                        a += '&nbsp;<button type="button" class="btn btn-primary" onclick="assignAccount(\'' + row.id + '\',\'' + noOfTxn + '\')">Assign Account</button>';
                    } else if ((userRoles.includes('21') || userRoles.includes('27')) && row.status == 'Awaiting Checker') {
                        a += '&nbsp;<button type="button" class="btn btn-primary" onclick="confirmAccount(\'' + row.id + '\',\'' + noOfTxn + '\')">Confirm</button>';
                    }
                    if ((userRoles.includes('21') || userRoles.includes('26')) && row.status == 'Awaiting Maker' && messageType == 'AccountMaintenance') {
                        a += '&nbsp;<button type="button" class="btn btn-primary" onclick="updateAccount(\'' + row.id + '\',\'' + noOfTxn + '\')">Update Account</button>';
                    }
                    if ((userRoles.includes('21') || userRoles.includes('26')) && row.status == 'Awaiting Maker' && row.message_type == 'AccountClosing') {
                        a += '&nbsp;<button type="button" class="btn btn-primary" onclick="closeAccount(\'' + row.id + '\',\'' + noOfTxn + '\')">Close Account</button>';
                    }
                    if (row.status != 'Rejected') {
                        a += '&nbsp;<button type="button" class="btn btn-danger" onclick="rejectAccount(\'' + row.id + '\')">Reject</button>';
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
        buttons: [{
                extend: "copy",
                title: 'MoFP BATCH ACCOUNTS',
                className: "btn-sm"
            }, {
                extend: "csv",
                title: 'MoFP BATCH ACCOUNTS',
                className: "btn-sm"
            }, {
                extend: "excel",
                title: 'MoFP BATCH ACCOUNTS',
                className: "btn-sm"
            }, {
                extend: "pdf",
                title: 'MoFP BATCH ACCOUNTS',
                className: "btn-sm"
            }, {
                extend: "print",
                title: 'MoFP BATCH ACCOUNTS',
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

function viewAttachments(messageId) {
    $("#loading").show();
    $.ajax({
        url: "/viewMoFPWorkflowAttachments",
        data: {messageId: messageId},
        type: 'GET',
        success: function (response) {
            $("#loading").hide();
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
            htmlData += '<div class="panel panel-default panel-fill" id="customer_kyc">';
            htmlData += '<div class="panel-heading no-print"><h4 class="panel-title">ATTACHMENT</h4></div>';
            htmlData += '<div class="panel-body">';
            htmlData += '<div class="col-md-12 pdf-content"></div>';
            htmlData += '</div>';
            htmlData += '</div>';
            htmlData += '</div>';
            $("#areaValue").html(htmlData);
            $(".pdf-content").append(obj);
        }
    });
}

function acceptAccount(id, nbOfTxn) {
    $("#loading").show();
    $.ajax({
        url: "/api/acceptMoFPAccount",
        type: 'POST',
        data: {id: id, nbOfTxn: nbOfTxn},
        success: function (res) {
            $("#loading").hide();
            if (res.success) {
                $("#successFlag").html('<div class="alert alert-success" role="alert"><button type="button" class="close" data-dismiss="alert" aria-label="Close">x</button><strong>Success!</strong></div>').show();
                $('#batchAccounts').DataTable().ajax.reload();
            } else
                $("#successFlag").html('<div class="alert alert-danger" role="alert"><button type="button" class="close" data-dismiss="alert" aria-label="Close">x</button><strong>Failed!</strong></div>').show();
        },
        error: function (res) {
            $("#loading").hide();
            $("#successFlag").html('<div class="alert alert-danger" role="alert"><button type="button" class="close" data-dismiss="alert" aria-label="Close">x</button><strong>Failed!</strong></div>').show();
            console.log(res);
        }
    })
}

function acceptRequest(id) {
    $("#loading").show();
    $.ajax({
        url: "/api/acceptMoFPAccountRequest",
        type: 'POST',
        data: {id: id},
        success: function (res) {
            $("#loading").hide();
            if (res.success) {
                $("#successFlag").html('<div class="alert alert-success" role="alert"><button type="button" class="close" data-dismiss="alert" aria-label="Close">x</button><strong>Success!</strong></div>').show();
                $('#accountsWorkflow').DataTable().ajax.reload();
            } else
                $("#successFlag").html('<div class="alert alert-danger" role="alert"><button type="button" class="close" data-dismiss="alert" aria-label="Close">x</button><strong>Failed!</strong></div>').show();
        },
        error: function (res) {
            $("#loading").hide();
            $("#successFlag").html('<div class="alert alert-danger" role="alert"><button type="button" class="close" data-dismiss="alert" aria-label="Close">x</button><strong>Failed!</strong></div>').show();
            console.log(res);
        }
    })
}

function sendRequest(id) {
    $("#loading").show();
    $.ajax({
        url: "/api/sendMoFPAccountRequestToBranch",
        type: 'POST',
        data: {id: id},
        success: function (res) {
            $("#loading").hide();
            if (res.success) {
                $("#successFlag").html('<div class="alert alert-success" role="alert"><button type="button" class="close" data-dismiss="alert" aria-label="Close">x</button><strong>Success!</strong></div>').show();
                $('#batchAccounts').DataTable().ajax.reload();
            } else
                $("#successFlag").html('<div class="alert alert-danger" role="alert"><button type="button" class="close" data-dismiss="alert" aria-label="Close">x</button><strong>Failed!</strong></div>').show();
        },
        error: function (res) {
            $("#loading").hide();
            $("#successFlag").html('<div class="alert alert-danger" role="alert"><button type="button" class="close" data-dismiss="alert" aria-label="Close">x</button><strong>Failed!</strong></div>').show();
            console.log(res);
        }
    })
}

function assignAccount(id, noOfTxn) {
    swal({
        title: 'Enter institution account',
        text: 'NB: Please open account in RUBIKON and proceed to enter account here!',
        html: `<input type="text" id="account" name="account" class="swal2-input" placeholder="Enter account" autocomplete="off" th:required="true">`,
        type: "question",
        confirmButtonText: 'Submit',
        confirmButtonColor: "#2b6331"
    })
    .then((result) => {
        if (result) {
            var account = $('#account').val();
            if (!account) {
                swal.showValidationMessage(`Please enter Account!`)
            } else {
                var formData = new FormData();
                formData.append("id", id);
                formData.append("accountNo", account);
                formData.append("nbOfTxn", noOfTxn);
                $("#loading").show();
                $.ajax({
                    url: "/api/assignMoFPAccount",
                    type: 'POST',
                    data: formData,
                    processData: false,
                    contentType: false,
                    success: function (res) {
                        $("#loading").hide();
                        if (res.success) {
                            $("#successFlag").html('<div class="alert alert-success" role="alert"><button type="button" class="close" data-dismiss="alert" aria-label="Close">x</button><strong>Success!</strong></div>').show();
                            $('#batchAccounts').DataTable().ajax.reload();
                        } else
                            $("#successFlag").html('<div class="alert alert-danger" role="alert"><button type="button" class="close" data-dismiss="alert" aria-label="Close">x</button><strong>Failed!</strong></div>').show();
                    },
                    error: function (res) {
                        $("#loading").hide();
                        $("#successFlag").html('<div class="alert alert-danger" role="alert"><button type="button" class="close" data-dismiss="alert" aria-label="Close">x</button><strong>Failed!</strong></div>').show();
                        console.log(res);
                    }
                })
            }
        }
    })
}

function confirmAccount(id, noOfTxn) {
    $("#loading").show();
    $.ajax({
        url: "/api/confirmMoFPAccount",
        type: 'POST',
        data: {id: id, nbOfTxn: noOfTxn},
        success: function (res) {
            $("#loading").hide();
            if (res.success) {
                $("#successFlag").html('<div class="alert alert-success" role="alert"><button type="button" class="close" data-dismiss="alert" aria-label="Close">x</button><strong>Success!</strong></div>').show();
                $('#batchAccounts').DataTable().ajax.reload();
            } else
                $("#successFlag").html('<div class="alert alert-danger" role="alert"><button type="button" class="close" data-dismiss="alert" aria-label="Close">x</button><strong>Failed!</strong></div>').show();
        },
        error: function (res) {
            $("#loading").hide();
            $("#successFlag").html('<div class="alert alert-danger" role="alert"><button type="button" class="close" data-dismiss="alert" aria-label="Close">x</button><strong>Failed!</strong></div>').show();
            console.log(res);
        }
    })
}

function updateAccount(id, noOfTxn) {
    swal({
        title: 'Update account',
        html: `<textarea id="comments" name="comments" placeholder="List summary of what has changed" rows="4" cols="50"/>`,
        type: "question",
        confirmButtonText: 'Submit',
        confirmButtonColor: "#2b6331"
    })
    .then((result) => {
        if (result) {
            var comments = $('#comments').val();
            if (!account) {
                swal.showValidationMessage(`Please enter summary of what has changed!`)
            } else {
                var formData = new FormData();
                formData.append("id", id);
                formData.append("nbOfTxn", noOfTxn);
                formData.append("comments", comments);
                $("#loading").show();
                $.ajax({
                    url: "/api/updateMoFPAccount",
                    type: 'POST',
                    data: formData,
                    success: function (res) {
                        $("#loading").hide();
                        if (res.success) {
                            $("#successFlag").html('<div class="alert alert-success" role="alert"><button type="button" class="close" data-dismiss="alert" aria-label="Close">x</button><strong>Success!</strong></div>').show();
                            $('#batchAccounts').DataTable().ajax.reload();
                        } else
                            $("#successFlag").html('<div class="alert alert-danger" role="alert"><button type="button" class="close" data-dismiss="alert" aria-label="Close">x</button><strong>Failed!</strong></div>').show();
                    },
                    error: function (res) {
                        $("#loading").hide();
                        $("#successFlag").html('<div class="alert alert-danger" role="alert"><button type="button" class="close" data-dismiss="alert" aria-label="Close">x</button><strong>Failed!</strong></div>').show();
                        console.log(res);
                    }
                })
            }
        }
    })
}

function closeAccount(id, noOfTxn) {
    $("#loading").show();
    $.ajax({
        url: "/api/closeMoFPAccount",
        type: 'POST',
        data: {id: id, nbOfTxn: noOfTxn},
        success: function (res) {
            $("#loading").hide();
            if (res.success) {
                $("#successFlag").html('<div class="alert alert-success" role="alert"><button type="button" class="close" data-dismiss="alert" aria-label="Close">x</button><strong>Success!</strong></div>').show();
                $('#batchAccounts').DataTable().ajax.reload();
            } else
                $("#successFlag").html('<div class="alert alert-danger" role="alert"><button type="button" class="close" data-dismiss="alert" aria-label="Close">x</button><strong>Failed!</strong></div>').show();
        },
        error: function (res) {
            $("#loading").hide();
            $("#successFlag").html('<div class="alert alert-danger" role="alert"><button type="button" class="close" data-dismiss="alert" aria-label="Close">x</button><strong>Failed!</strong></div>').show();
            console.log(res);
        }
    })
}

function rejectRequest(id) {
    swal({
        title: 'Reject request',
        html: `<textarea id="comments" name="comments" placeholder="Enter rejection comments" rows="4" cols="50"/>`,
        type: "question",
        confirmButtonText: 'Submit',
        confirmButtonColor: "#2b6331"
    })
    .then((result) => {
        if (result) {
            var comments = $('#comments').val();
            if (!account) {
                swal.showValidationMessage(`Please enter rejection reason!`)
            } else {
                var formData = new FormData();
                formData.append("id", id);
                formData.append("comments", comments);
                $("#loading").show();
                $.ajax({
                    url: "/api/rejectMoFPRequest",
                    type: 'POST',
                    data: formData,
                    success: function (res) {
                        $("#loading").hide();
                        if (res.success) {
                            $("#successFlag").html('<div class="alert alert-success" role="alert"><button type="button" class="close" data-dismiss="alert" aria-label="Close">x</button><strong>Success!</strong></div>').show();
                            $('#accountsWorkflow').DataTable().ajax.reload();
                        } else
                            $("#successFlag").html('<div class="alert alert-danger" role="alert"><button type="button" class="close" data-dismiss="alert" aria-label="Close">x</button><strong>Failed!</strong></div>').show();
                    },
                    error: function (res) {
                        $("#loading").hide();
                        $("#successFlag").html('<div class="alert alert-danger" role="alert"><button type="button" class="close" data-dismiss="alert" aria-label="Close">x</button><strong>Failed!</strong></div>').show();
                        console.log(res);
                    }
                })
            }
        }
    })
}

function rejectAccount(id, noOfTxn) {
    swal({
        title: 'Reject account',
        html: `<textarea id="comments" name="comments" placeholder="Enter rejection comments" rows="4" cols="50"/>`,
        type: "question",
        confirmButtonText: 'Submit',
        confirmButtonColor: "#2b6331"
    })
    .then((result) => {
        if (result) {
            var comments = $('#comments').val();
            if (!account) {
                swal.showValidationMessage(`Please enter rejection reason!`)
            } else {
                var formData = new FormData();
                formData.append("id", id);
                formData.append("nbOfTxn", noOfTxn);
                formData.append("comments", comments);
                $("#loading").show();
                $.ajax({
                    url: "/api/rejectMoFPAccount",
                    type: 'POST',
                    data: formData,
                    success: function (res) {
                        $("#loading").hide();
                        if (res.success) {
                            $("#successFlag").html('<div class="alert alert-success" role="alert"><button type="button" class="close" data-dismiss="alert" aria-label="Close">x</button><strong>Success!</strong></div>').show();
                            $('#batchAccounts').DataTable().ajax.reload();
                        } else
                            $("#successFlag").html('<div class="alert alert-danger" role="alert"><button type="button" class="close" data-dismiss="alert" aria-label="Close">x</button><strong>Failed!</strong></div>').show();
                    },
                    error: function (res) {
                        $("#loading").hide();
                        $("#successFlag").html('<div class="alert alert-danger" role="alert"><button type="button" class="close" data-dismiss="alert" aria-label="Close">x</button><strong>Failed!</strong></div>').show();
                        console.log(res);
                    }
                })
            }
        }
    })
}

function enrollMobileBanking(accountNo) {
    swal({
        title: "Are you sure you want to enroll this account to Mobile Banking?",
        type: "question",
        showCancelButton: true,
        confirmButtonText: 'Submit',
        confirmButtonColor: "#3ba03e"
    })
    .then((result) => {
        if (result) {
            swal.close();
            $("#loading").show();
            $.ajax({
                url: "/enrollMobileBanking",
                data: {acctNo: accountNo},
                type: 'POST',
                success: function (response) {
                    $("#loading").hide();
                    if (response.success) {
                        swal("Success!", "This account has been enrolled to mobile banking successfully!", "success");
                        $('#kycAccounts').DataTable().ajax.reload();
                    } else {
                        swal("Error!", "There was an error in enrolling mobile banking for this user, please try again...", "error");
                    }
                }
            });
        }
    });
}

function resetMobileChannelUser(accountNo) {
    swal({
        title: "Are you sure you want to reset Mobile Banking PIN for this account?",
        type: "question",
        showCancelButton: true,
        confirmButtonText: 'Submit',
        confirmButtonColor: "#3ba03e"
    })
    .then((result) => {
        if (result) {
            swal.close();
            $("#loading").show();
            $.ajax({
                url: "/api/resetMobileChannelUser?payLoad=" + accountNo,
                type: 'GET',
                success: function (response) {
                    $("#loading").hide();
                    if (response.success) {
                        swal("Success!", "PIN has been reset successfully!", "success");
                    } else {
                        swal("Error!", "There was an error in resetting PIN for this user, please try again...", "error");
                    }
                }
            });
        }
    });
}

function assignStaffVisits(userId) {
    $("#loading").show();
    $.ajax({
        url: "/api/assignStaffVisits",
        data: {userId: userId},
        type: 'POST',
        success: function (response) {
            $("#loading").hide();
            $("#myModal").modal("show");
            var htmlData = '<div class="row">';
            htmlData += '<div class="panel panel-default panel-fill">';
            htmlData += '<div class="panel-heading"><h4 class="panel-title">ASSIGN STAFF VISITS</h4></div>';
            htmlData += '<div class="panel-body">';
            htmlData += '<div class="col-md-12">';
            htmlData += '<form class="form-horizontal" role="form" id="assign_staff_visits" method="POST">';
            htmlData += '<div class="row">';
            htmlData += '<div class="col-md-4">';
            htmlData += '<label>Staff Name</label>';
            htmlData += '<select class="form-control form-control-sm col-sm-12 select2" id="staffName" name="staff_name"';
            htmlData += ' autocomplete="off" style="font-size: 10px !important;">';
            htmlData += '</select>';
            htmlData += '</div>';
            htmlData += '</div>';
            htmlData += '<div class="row">';
            htmlData += '<div class="col-md-4">';
            htmlData += '<label>Agent Device</label>';
            htmlData += '<select class="form-control form-control-sm col-sm-12 select2" id="agentDevice" name="agent_device"';
            htmlData += ' autocomplete="off" style="font-size: 10px !important;">';
            htmlData += '</select>';
            htmlData += '</div>';
            htmlData += '</div>';
            htmlData += '<div class="row text-center">';
            htmlData += '<button type="button" class="btn btn-primary assign-staff-visit" onclick="assignStaffVisit(' + userId + ')">';
            htmlData += 'Assign Staff';
            htmlData += '</button>';
            htmlData += '</div>';
            htmlData += '</form>';
            htmlData += '</div>';
            htmlData += '</div>';
            htmlData += '</div>';
            htmlData += '</div>';
            $("#areaValue").html(htmlData);
            $('#staffName').select2({
                placeholder: 'Search staff name',
                ajax: {
                    url: '/',
                    dataType: 'json',
                    delay: 250,
                    data: function (data) {
                        return {
                            searchTerm: data.term
                        };
                    },
                    processResults: function (response) {
                        return {
                            results: response
                        };
                    },
                    cache: true
                }
            });
            $('#agentDevice').select2({
                placeholder: 'Search agent device',
                ajax: {
                    url: '/',
                    dataType: 'json',
                    delay: 250,
                    data: function (data) {
                        return {
                            searchTerm: data.term
                        };
                    },
                    processResults: function (response) {
                        return {
                            results: response
                        };
                    },
                    cache: true
                }
            });
        }
    });
}
