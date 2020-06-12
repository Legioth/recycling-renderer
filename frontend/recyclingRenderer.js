console.log("loaded");

let nextId = 0;

window.recyclingRenderer = {}

class RecyclingRenderer extends HTMLElement {
  constructor() {
    super();
    this.rendererId = nextId++;
  }
  
  connectedCallback() {
    this.textContent = "Initing";
    window.recyclingRenderer[this.rendererId] = this;
  }
  
  disonnectedCallback() {
    console.log("TODO: send cleanup message to the server", this);
    delete window.recyclingRenderer[this.idrendererId];
  }
  
  setNodeId(nodeId) {
    let contentNode = window.Vaadin.Flow.clients[this.appId].getByNodeId(+nodeId)
    if (!contentNode) {
      console.error("Could not find node",nodeId)
      return;
    }
    
    if (contentNode.parentElement != this) {
      this.textContent = '';
      this.appendChild(contentNode);
    }    
  }
  
  setKey(key) {
    if (this.key === key) {
      return;
    }
    
    let oldKey = this.key;
    this.key = key;
    console.log("Requesting", oldKey, key);
    
    if (oldKey == null) {
      this.channel(oldKey, key, this.rendererId);
    } else {
      this.channel(oldKey, key);
    }
  }
}

customElements.define("recycling-renderer", RecyclingRenderer);

window.recyclingRenderFactory = (appId, channel) => {
  return (cell, column, data) => {
    let element;
    if (cell.childElementCount == 0) {
      element = document.createElement("recycling-renderer");
      element.appId = appId;
      element.channel = channel;
      cell.appendChild(element);
    } else {
      element = cell.firstElementChild;
    }
    element.setKey(data.item.key);
  }
} 