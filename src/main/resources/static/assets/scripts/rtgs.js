$(document).ready(function () {
    $('#preloader').hide();
    console.log('here we are');
    $('[data-toggle="tab"]').click(function (e) {
        $("#preloader").show();
        var $this = $(this),
                loadurl = $this.attr('data-url');
        $.ajax({
            url: loadurl,
            type: 'GET',
            success: function (res) {
                $("#preloader").hide();
                $("#tabContent").html(res).return;
            }
        });
//        return true;
    });
    //get account details
    $('button[type=button]').click(function (e) {
        var accountNo = $("input[name='senderAcct']").val();
        console.log(accountNo);
    });
//    function getAccountDetails() {
//        var accountNo = $("input[name='senderAcct']").val();
//        console.log(accountNo);
//    }
});

