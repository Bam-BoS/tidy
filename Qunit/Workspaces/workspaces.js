document.addEventListener('DOMContentLoaded', function() {
  const workspaceNav = document.getElementById('workspaceNav');
  const content = document.getElementById('content');
  let workspaceCount = 2;

  workspaceNav.addEventListener('click', function(e) {
    if (e.target.dataset.sohno === 'new') {
      e.preventDefault();
      workspaceCount++;
      
      const newSection = document.createElement('section');
      newSection.dataset.sohcanv = 'workspace';
      newSection.dataset.sohgroup = 'workspace';
      newSection.dataset.sohno = workspaceCount;
      newSection.textContent = `Body #${workspaceCount}`;
      content.appendChild(newSection);
      
      const newLink = document.createElement('a');
      newLink.href = `#w${workspaceCount}`;
      newLink.dataset.sohbtn = 'workspace';
      newLink.dataset.sohgroup = 'workspace';
      newLink.dataset.sohno = workspaceCount;
      newLink.textContent = workspaceCount;
      
      workspaceNav.insertBefore(document.createTextNode(' '), e.target);
      workspaceNav.insertBefore(newLink, e.target);
      workspaceNav.insertBefore(document.createTextNode(' '), e.target);
    }
  });
});