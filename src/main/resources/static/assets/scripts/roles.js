function selectOpRole() {
    $("#loading").show();
    $.ajax({
        url: "/selectOpRole",
        type: 'POST',
        success: function (response) {
            $("#loading").hide();
            //$("#cbsTxns").show();
            $("#myModal").modal("show");
            $("#areaValue").html(response);
        }
    });
}
function setPostingRole(selectedRole){
   alert(selectedRole);
}