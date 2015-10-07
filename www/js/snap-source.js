/**
 * Created by skywalker on 9/23/15.
 */

var app = angular.module("snap-source", ['ui.bootstrap', 'angularjs-dropdown-multiselect']);
app.host = window.location.href.replace("/static/index.html","").replace("http://", "");
app.endpoint = "http://" + app.host + "/snap-sources";

app.controller('MainController', function($scope) {
    $scope.closeAlert = function(index) {
        $scope.alerts.splice(index, 1);
    };
    $scope.alert = {
        message : ""
    };
    $scope.showNewSource = function() {
        showNewSource();
    };
    $scope.closeNewSource = function() {
        closeNewSource();
    };
    $scope.moduleData = [
        {
            id : 1,
            label : "Link"
        },
        {
            id : 2,
            label : "Domain"
        },
        {
            id : 3,
            label : "RSS"
        },
        {
            id : 4,
            label : "Rest"
        },
        {
            id : 5,
            label : "Twitter"
        }
    ];
    $scope.moduleModel = {};
    $scope.moduleText = {
        buttonDefaultText: "Module"
    };
    $scope.moduleSettings = {
        showUncheckAll : false,
        selectionLimit: 1,
        smartButtonMaxItems: 1
    };
    $scope.moduleEvents = {
        onItemSelect : function(e) {
            var id = $scope.moduleModel.id;
            var module = "";
            switch(id) {
                case 1:
                    module = "link";
                    break;
                case 2:
                    module = "domain";
                    break;
                case 3:
                    module = "rss";
                    break;
                case 4:
                    module = "rest";
                    break;
                case 5:
                    module = "twitter";
                    break;
                default:
                    break;
            }
            if (module != "") $scope.module = module;
        }
    };
    $scope.pauseData = [
        {
            id : 1,
            label : "15 mins"
        },
        {
            id : 2,
            label : "30 mins"
        },
        {
            id : 3,
            label : "1 hour"
        },
        {
            id : 4,
            label : "6 hours"
        },
        {
            id : 5,
            label : "12 hours"
        },
        {
            id : 6,
            label : "1 day"
        },
        {
            id : 7,
            label : "1 week"
        }
    ];
    $scope.pauseModel = {};
    $scope.pauseText = {
        buttonDefaultText: "Pause"
    };
    $scope.pauseSettings = {
        showUncheckAll : false,
        selectionLimit: 1,
        smartButtonMaxItems: 1
    };
    $scope.pauseEvents = {
        onItemSelect : function(e) {
            var id = $scope.pauseModel.id;
            var pause = "";
            switch(id) {
                case 1:
                    pause = "15m";
                    break;
                case 2:
                    pause = "30m";
                    break;
                case 3:
                    pause = "1h";
                    break;
                case 4:
                    pause = "6h";
                    break;
                case 5:
                    pause = "12h";
                    break;
                case 6:
                    pause = "1d";
                    break;
                case 7:
                    pause = "7d"
                default:
                    break;
            }
            $scope.pause = pause;
        }
    };
    $scope.addSource = function() {
        addSource($scope);
    };
    $scope.delete = function(sid) {

    };
    $scope.edit = function(sid) {
        edit(sid);
    };

    app.scope = $scope;
    sourcesList($scope);
});

function module2id(value) {
    switch (value) {
        case "link":
            return 1;
        case "domain":
            return 2;
        case "rss":
            return 3;
        default :
            return 1;
    }
    return 1;
}

function pause2id(value) {
    switch (value) {
        case "15m":
            return 1;
        case "30m":
            return 2;
        case "1h":
            return 3;
        case "6h":
            return 4;
        case "12h":
            return 5;
        case "1d":
            return 6;
        case "7d":
            return 7;
        default :
            return 3;
    }
    return 3;
}

function notify(alert) {
    $.notify(alert.msg, alert.type, {
        autoHide: true,
        autoHideDelay: 3000
    });
}

function del(sid) {
    var source = app.data[sid];
    if (source.status == "running") {
        notify({
            type : "error",
            msg : "Cannot delete source " + source.sid + ". Still running. Stop it and try again. Thanks"
        })
    } else {
        $.get(app.endpoint + "/delete/" + sid, function (response) {
            notify({
                type : "success",
                msg : "Source " + sid + " delete"
            });
            app.data[sid] = {};
            sourcesList(app.$scope);
        }).fail(function(e){
            notify({
                type : "error",
                msg : "Cannot delete source with id " + sid
            })
        });
    }

}

function stop(sid) {
    var source = app.data[sid];
    if (source.status != "running" && source.status != "paused") {
        notify({
            type : "error",
            msg : "Cannot stop source " + source.sid + ". It is not running."
        })
    } else {
        $.get(app.endpoint + "/stop/" + sid, function (response) {
            notify({
                type : "success",
                msg : "Source " + sid + " stopped"
            });
            app.data[sid] = {};
            sourcesList(app.$scope);
        }).fail(function(e){
            notify({
                type : "error",
                msg : "Cannot stop source with id " + sid
            })
        });
    }
}

function pause(sid) {
    var source = app.data[sid];
    if (source.status != "running") {
        notify({
            type : "error",
            msg : "Cannot pause source " + source.sid + ". It is not running"
        })
    } else {
        $.get(app.endpoint + "/pause/" + sid, function (response) {
            notify({
                type : "success",
                msg : "Source " + sid + " paused"
            });
            app.data[sid] = {};
            sourcesList(app.$scope);
        }).fail(function(e){
            notify({
                type : "error",
                msg : "Cannot pause source with id " + sid
            })
        });
    }

}

function play(sid) {
    var source = app.data[sid];
    if (source.status == "ready" || source.status == "stopped") {
        $.get(app.endpoint + "/start/" + sid, function (response) {
            notify({
                type : "success",
                msg : "Source " + sid + " running"
            });
            app.data[sid] = {};
            sourcesList(app.$scope);
        }).fail(function(e){
            notify({
                type : "error",
                msg : "Cannot run source with id " + sid
            })
        });
    } else if (source.status == "paused") {
        $.get(app.endpoint + "/resume/" + sid, function (response) {
            notify({
                type : "success",
                msg : "Source " + sid + " resumed"
            });
            app.data[sid] = {};
            sourcesList(app.$scope);
        }).fail(function(e){
            notify({
                type : "error",
                msg : "Cannot resume source with id " + sid
            })
        });
    } else {
        notify({
            type : "error",
            msg : "Cannot run source " + source.sid + ". Is it running?"
        })
    }
}

function edit(sid) {
    var $scope = app.scope;
    var source = app.data[sid];
    if (source) {
        $("#source-id").val(source.sid);
        $("#source-name").val(source.name);
        $("#source-author").val(source.author);
        $("#source-description").val(source.description);
        $("#source-sources").val(source.params.urls.replace(/,/g, "\n"));
        $("#source-loop").attr("checked", source.params.loop);
        $scope.pauseModel = {
            id : pause2id(source.params.pause)
        };
        $scope.moduleModel = {
            id : module2id(source.module)
        };
        $scope.pause = source.pause;
        $scope.module = source.module;
        showNewSource();
    }
}


function showNewSource() {
    $("#source-new").show(500);
}

function closeNewSource() {
    $("#source-new").hide(500);
}

function sourcesList($scope) {
    $.get(app.endpoint + "/read-all", function(response) {
        var data = JSON.parse(response);
        notify({
            type : "success",
            msg : "Found " + data.length + " sources"
        });
        app.data = {};
        for (var i = 0; i < data.length; i++) {
            app.data[data[i].sid] = data[i];
        }
        addSources(data);
    }).fail(function(response) {

    });
}

function addSource($scope) {
    var sources = $("#source-sources").val();
    var urls = text2list(sources);
    if (urls.length > 0) {
        var body = {
            name: $("#source-name").val(),
            author: $("#source-author").val(),
            description: $("#source-description").val(),
            params : {
                loop: $("#source-loop").is(':checked'),
                pause: $scope.pause,
                urls: urls.join()
            },
            module: $scope.module
        };
        var sid = $("#source-id").val();
        if (sid && sid != "") body.sid = sid;
        $.ajax({
            type: 'POST',
            url: app.endpoint + "/create",
            data: JSON.stringify(body), // or JSON.stringify ({name: 'jonas'}),
            success: function (data) {
                closeNewSource();
                notify({
                    type : "success",
                    msg : "Added " + data.name + ", " + data.author + " [" + data.sid + "]"
                });
                sourcesList();
            },
            contentType: "application/json",
            dataType: 'json'
        });
    } else {
        notify({
            type : "danger",
            msg : "Something is wrong in your sources list"
        })
    }
}

function occurrences(string, subString, allowOverlapping){

    string+=""; subString+="";
    if(subString.length<=0) return string.length+1;

    var n=0, pos=0;
    var step=allowOverlapping?1:subString.length;

    while(true){
        pos=string.indexOf(subString,pos);
        if(pos>=0){ ++n; pos+=step; } else break;
    }
    return n;
}

function text2list(text) {
    var result = [];
    var items = text.split("\n");
    for (var i = 0; i < items.length; i++) {
        var item = items[i];
        if (occurrences(item, "http") > 1) return [];
        result.push(item);
    }
    return result;
}

var $TABLE = $('#source-table');

$('.table-add').click(function () {
    var $clone = $TABLE.find('tr.hide').clone(true).removeClass('hide table-line');
    $TABLE.find('table').append($clone);
});

$('.source-table-remove').click(function () {
    $(this).parents('tr').detach();
});

$('.source-table-up').click(function () {
    var $row = $(this).parents('tr');
    if ($row.index() === 1) return; // Don't go above the header
    $row.prev().before($row.get(0));
});

$('.source-table-down').click(function () {
    var $row = $(this).parents('tr');
    $row.next().after($row.get(0));
});


function addSources(sources) {
    var $rows = $TABLE.find('tr');
    var l = $rows.length;
    for (var i = 0; i < l; i++) {
        var $row = $rows[i];
        if ($row.className != "headers" && $row.className != "hide") {
            $row.remove();
        }
    }

    if (sources) {
        for (var j = 0; j < sources.length; j++) {
            var record = sources[j];
            var $clone = $TABLE.find('tr.hide').clone(true).removeClass('hide table-line');
            $clone[0].cells[0].innerHTML = record.sid;
            $clone[0].cells[1].innerHTML = record.name;
            $clone[0].cells[2].innerHTML = record.module;
            $clone[0].cells[3].innerHTML = record.author;
            $clone[0].cells[4].innerHTML = record.description;
            $clone[0].cells[5].innerHTML = record.params.loop || false;
            $clone[0].cells[6].innerHTML = record.params.pause || "1h";
            $clone[0].cells[7].innerHTML = record.status;
            $clone[0].cells[8].innerHTML = '<p data-placement="top" data-toggle="tooltip" title="Edit"><button class="btn btn-primary btn-xs" data-title="Edit" data-toggle="modal" data-target="#edit" onclick="edit(\'' + record.sid +  '\');"><span class="glyphicon glyphicon-pencil"></span></button></p>'
            $clone[0].cells[9].innerHTML = '<p data-placement="top" data-toggle="tooltip" title="Delete"><button class="btn btn-danger btn-xs" data-title="Delete" data-toggle="modal" data-target="#delete" onclick="del(\'' + record.sid +  '\');"><span class="glyphicon glyphicon-trash"></span></button></p>'
            $clone[0].cells[10].innerHTML = '<p data-placement="top" data-toggle="tooltip" title="Player"><button class="btn btn-danger btn-xs" data-title="Stop" data-toggle="modal" data-target="#delete" onclick="stop(\'' + record.sid +  '\');"><span class="glyphicon glyphicon-stop"></span></button><button class="btn btn-success btn-xs" data-title="Pause" data-toggle="modal" data-target="#pause" onclick="pause(\'' + record.sid +  '\');"><span class="glyphicon glyphicon-pause"></span></button><button class="btn btn-info btn-xs" data-title="Play" data-toggle="modal" data-target="#play" onclick="play(\'' + record.sid +  '\');"><span class="glyphicon glyphicon-play"></span></button></p>'
            $TABLE.find('table').append($clone);
        }
    }
}