const listContainer = document.querySelector('#service-list');
let servicesRequest = new Request('/service');
fetch(servicesRequest)
.then(function(response) { return response.json(); })
.then(function(serviceList) {
  serviceList.forEach(service => {
    var li = document.createElement("li");
    var deleteButton = document.createElement("button");
    deleteButton.innerHTML = "delete"; // Text will not show up :'(
    deleteButton.type = "button";
    var icon = document.createElement("i");
    icon.classList.add("fa fa-close");
    deleteButton.appendChild(icon);
    deleteButton.onclick = deleteService(service);
    
    li.appendChild(document.createTextNode(service.name + ': ' + service.status));
    //li.appendChild(deleteButton);
    listContainer.appendChild(li);
  });
});

const saveButton = document.querySelector('#post-service');
saveButton.onclick = evt => {
    let urlName = document.querySelector('#url-name').value;
    fetch('/service', {
    method: 'post',
    headers: {
    'Accept': 'application/json, text/plain, */*',
    'Content-Type': 'application/json'
    },
  body: JSON.stringify({url:urlName})
}).then(res=> location.reload());
}

function deleteService(service){
    fetch('/service', {
      method: 'delete',
      headers: {
        'Accept': 'application/json, text/plain, */*',
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({url:service.name})
    }).then(res => location.reload());
}