
function previewEditSProvider(sproviderId) {
    $("#loading").show();
    $.ajax({
        url: "/firePreviewEditSP",
        data: {sproviderId: sproviderId},
        type: 'POST',
        success: function (response) {
            $("#loading").hide();
            $("#myModal").modal("show");
            $("#areaValue").html(response);
        }
    });
}




   $("#updateServiceProviderbtn").click(function (e) {
            $("#updateServiceProviderbtn").attr('disabled',true);
                e.preventDefault();
                $('#preloader2').show();
                var formdata = new FormData($("#updateServiceProviderForm")[0]);
                var spId = $('#spId').val();
                console.log(spId);
                formdata.append("spId",spId);
                $.ajax({
                    url: '/fireUpdateServiceProvider',
                    data: formdata,
                    type: 'POST',
                    processData: false,
                    contentType: false,
                    success: function (res) {
                        $("#preloader2").hide();
                        if(res.status == "SUCCESS"){
                           $("#successFlag").html('<div class="alert alert-success" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"success-label="Close"> </button><strong>'+res.result+'</strong></div>');
                        }else if(res.status == "ERROR"){
                           $("#successFlag").html('<div class="alert alert-danger" role="alert" id="successFlag"><button type="button" class="close" data-dismiss="alert"danger-label="Close"> </button><strong>'+res.result+'</strong></div>');
                        }else if(res.status == "FAIL"){
                              $("#updateServiceProviderbtn").attr('disabled',false);

                            $.each(res.result,function(key,value){
                                $('[name = '+ key +']').after('<span class="error text-danger">'+value+'</span>');
                            });
                        }
                    }
                });
            });



