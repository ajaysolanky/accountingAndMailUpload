$(document).ready(function(){
//    var data = $(".csvFiles input:checkbox:checked").map(function() {
//        var $parent = $(this).closest(".csvFiles");
//        return {
//            name: $parent.next(".name").text(),
//            email: $parent.siblings(".emayl").text()
//        };
//    }).get();

    $("#submitData").click(function(){
        var userResponse = $(".csvFiles:checkbox:checked").map(function() {
            return {
                file: $(this).attr("name"),
                template: $(this).next().closest(".templateSelect").val()
            };
        }).get();
        $("button").hide();
        $.post("processMailFiles",{"seanp" : userResponse}, function() {
            document.location.href="viewUploads?type=mail";
//            $("body").load("viewUploads",{"type":"mail"}, function() {
//                console.log("success!");
//            });
        });
//        setTimeout(function() {
//            document.location.href="viewUploads";
//        },500);
    });


    function sds (callback) {
        callback();
    }

    var filesJson = $.parseJSON($("#relFiles").html());

    var currOption;
    for(var i = 0; i < filesJson["csv"].length;i++) {
        currOption = "<input class=\"csvFiles\" type=\"checkbox\" name=\""+filesJson["csv"][i]["file name"]+"\">"+filesJson["csv"][i]["file name"]+"<select class=\"templateSelect\">";
        for(var j = 0; j < filesJson["templates"].length; j++) {
            currOption+="<option value=\""+filesJson["templates"][j]["file name"]+"\">"+filesJson["templates"][j]["file name"]+"</option>";
        }
        currOption+="</select><br>";
        $("#dpFiles").append(currOption);
    }
});