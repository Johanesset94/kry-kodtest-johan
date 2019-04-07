var listContainer = document.querySelector('#service-list');
var servicesRequest = new Request('/service');
fetch(servicesRequest)
    .then(function (response) {
        return response.json();
    })
    .then(function (serviceList) {
        serviceList.forEach(function (service) {
            var li = document.createElement("li");

            var deleteButton = document.createElement("button");
            deleteButton.innerHTML = "delete";
            deleteButton.type = "button";
            deleteButton.onclick = function () {
                deleteService(service)
            };

            var promptButton = document.createElement("button");
            promptButton.innerHTML = "Set name";
            promptButton.onclick = function(){
                var newName = prompt("What should this service be called");
                service.name = newName;
                updateService(service);
            }
            if(service.name !== null){
                console.log("got name");
                li.appendChild(document.createTextNode(service.name +
                    ' url: ' + service.url +
                    ' | status:' + service.status +
                    ' | added: ' + service.date +
                    '  '));
            }else{
                console.log("no name");
                li.appendChild(document.createTextNode(
                    'url: ' + service.url +
                    ' | status:' + service.status +
                    ' | added: ' + service.date +
                    '  '));
            }
            li.appendChild(deleteButton);
            li.appendChild(promptButton);
            listContainer.appendChild(li);
        });
    });

var saveButton = document.querySelector('#post-service');
saveButton.onclick = function () {
    var urlName = document.querySelector('#url-name').value;
    fetch('/service', {
        method: 'post',
        headers: {
            'Accept': 'application/json, text/plain, */*',
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({url: urlName})
    }).then(function () {
        location.reload()
    });
}

function deleteService(service) {
    fetch('/service', {
        method: 'delete',
        headers: {
            'Accept': 'application/json, text/plain, */*',
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(service)
    }).then(function () {
        location.reload()
    });
}

function updateService(service) {
    fetch('/service', {
        method: 'put',
        headers: {
            'Accept': 'application/json, text/plain, */*',
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(service)
    }).then(function () {
        location.reload()
    });
}