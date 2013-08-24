//Authored by Ajay Solanky
//Copyright Julep Beauty Inc. 2013

$(document).ready(function() {
    $("#loading").hide();
    $("#status").hide();
    $("#failures").hide();
    $("#success").hide();

    $("#reset").click(function(){
        $("#status").hide();
        $("#failures").hide();
        $("#success").hide();
        $.post("reset");
        $("#uploadTable").html("<tr class=\"uploadRow\"><td>File Name</td><td>Date Uploaded</td><td>Processing Time</td><td>Type</td><td>Record Count</td><td>Error Count</td><td>Skip Count</td><td>Delete</td></tr>");
    });

    $(".fileSubmit").click(function(){
        $("#status").hide();
        $("#failures").hide();
        $("#statusDiv").attr("style","height:auto;");
        $("#success").hide();
        $("#loading").show();
        var loading = loadingBar();
        $(".fileSubmit").hide();
        updateTable(function(){
            $("loading").hide();
            $(".fileSubmit").show();
            clearInterval(loading);
            displayErrors();
            $("#status").show();
        });
    });

    $(".delete").click(function(){
        $("#reset").hide();
        $.post("deleteUploadAction",{"uploadId": $(this).attr("id")});
        setTimeout(
            updateTable(function(){
//                    $("#loading").hide();
                $("#reset").show();
//                    clearInterval(loading);
            }),500);
        $(this).parent().parent().remove();
    });

    function loadingBar() {
        var progress = 0;

        return setInterval(function(){
            progress = progress+.1;
            if(progress>=1){
                progress=0;
            }
            $("#progress").attr("value",progress);
        },500);
    }

    function updateDisplay() {
        $(".delete").click(function(){
            $("#reset").hide();
            $.post("deleteUploadAction",{"uploadId": $(this).attr("id")});
            setTimeout(
                updateTable(function(){
//                    $("#loading").hide();
                    $("#reset").show();
//                    clearInterval(loading);
                }),500);
            $(this).parent().parent().remove();
        });
    }

    function displayErrors() {
        var batch_id = $("#upload_target").contents().text();
        $("#frameContainer").html("<iframe id=\"upload_target\" style=\"visibility: hidden\"></iframe>");
        $.getJSON("jsonErrorTable",{batch : batch_id}, function(results) {
            var errorsArr = results["errors"];
            if(errorsArr.length==0){
                $("#success").show();
            }
            else{
                var innerHtml;
                for (var i =0; i < errorsArr.length; i++) {
                    innerHtml+="<tr><td class=\"fileColumn\">"+errorsArr[i]["file_name"]+"</td><td class=\"messageColumn\">"+errorsArr[i]["message"]+"</td></tr>";
                }
                $("#errorTable").html(innerHtml);
                if (errorsArr.length>10){
                    $("#statusDiv").attr("style","height:200px; overflow:auto;");
                }
                $("#failures").show();
            }
        });
    }

    // var counter = 0;
    function updateTable (callback){
        $("#upload_target").load(function(){
            var batch_id = $("#upload_target").contents().text();
            var clear = setInterval(function(){
                // console.log("blackstar");
                $.getJSON("jsonUploadTable", {batch : batch_id} ,function(results) {
    //                    $("#loading").show();
                    // console.log("kweli "+counter);
    //                    console.log(results);
                    // counter++;
    //                    console.log(results.uploads.length);
                    if($("#pageType").text()!="mail" && ($("#header").html()!="Accounting File Upload" || results.uploads.length ==0 || results.uploads[results.uploads.length-1]["Processing Time"] != "Processing/Not yet processed")){
                        // console.log("mos");
                        buildTable(batch_id,results.uploads);
                        $("#loading").hide();
                        clearInterval(clear);
                        callback();
                    }
                    buildTable(batch_id,results.uploads);
                });
            },1000);
        });
    }

    var currentBatches = new Array(); //contains all the current batches that should be displayed on the screen
    function buildTable(batch,results) {
        var isContained = false;
        for (var h = 0; h < currentBatches.length; h++) {
            if (currentBatches[h]==batch) {
                isContained = true;
                break;
            }
        }
        if (!isContained) {
            currentBatches.push(batch);
            if(results==null) return;
            var tableString;
            // var tableString = "<tr class=\"uploadRow\"><td>File Name</td><td>Date Uploaded</td><td>Processing Time</td><td>Record Count</td><td>Error Count</td><td>Skip Count</td><td>Delete</td></tr>";
            for (var i=0; i< results.length; i++){
                tableString+="<tr class=\"uploadRow\" id=\"row"+i+"batch"+batch+"\"><td>"+results[i]["File Name"]+"</td><td>"+results[i]["Date Uploaded"]+"</td><td>"+results[i]["Processing Time"]+"</td><td>"+results[i]["Type"]+"</td><td>"+results[i]["Record Count"]+"</td><td><a href=\"viewErrorsAction\">"+results[i]["Error Count"]+"</a></td><td>"+results[i]["Skip Count"]+"</td><td><button id=\""+results[i]["id"]+"\" class=\"delete\">Delete</button></td></tr>";
            }
            $("#uploadTable").append(tableString);
        }
        else {
            for (var j=0; j<results.length;j++){
                $("#row"+j+"batch"+batch).html("<td>"+results[j]["File Name"]+"</td><td>"+results[j]["Date Uploaded"]+"</td><td>"+results[j]["Processing Time"]+"</td><td>"+results[j]["Type"]+"</td><td>"+results[j]["Record Count"]+"</td><td><a href=\"viewErrorsAction\">"+results[j]["Error Count"]+"</a></td><td>"+results[j]["Skip Count"]+"</td><td><button id=\""+results[j]["id"]+"\" class=\"delete\">Delete</button></td>");
            }
        }
        updateDisplay();
    }
});