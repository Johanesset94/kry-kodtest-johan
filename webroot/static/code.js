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
            }

            li.appendChild(document.createTextNode(service.name + ': ' + service.status + '   '));
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
        body: JSON.stringify({url: service.name})
    }).then(function () {
        location.reload()
    });
}