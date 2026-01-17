package com.sentinelrss.server

object DashboardHtml {
    fun getHtml(): String {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>SentinelRSS</title>
                <style>
                    body { background-color: #121212; color: #e0e0e0; font-family: sans-serif; }
                    .container { max-width: 800px; margin: 0 auto; padding: 20px; }
                    .article { background-color: #1e1e1e; padding: 15px; margin-bottom: 10px; border-radius: 5px; }
                    .article h2 { margin-top: 0; }
                    a { color: #bb86fc; text-decoration: none; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h1>SentinelRSS Dashboard</h1>
                    <div id="status" style="margin-bottom: 10px; color: #888;">Checking ML Status...</div>
                    <button onclick="refreshFeeds()" style="margin-bottom: 20px;">Refresh Feeds</button>
                    <div id="articles">Loading...</div>
                </div>
                <script>
                    function checkStatus() {
                        fetch('/api/status')
                            .then(res => res.json())
                            .then(data => {
                                const el = document.getElementById('status');
                                if (data.ml_model_loaded) {
                                    el.innerHTML = '<span style="color: #4caf50;">● ML Model Active</span>';
                                } else {
                                    el.innerHTML = '<span style="color: #ff9800;">● ML Model Missing (Using Fallback Mode)</span> <button onclick="downloadModel()" style="margin-left:10px; font-size: 0.8em;">Download Model (3MB)</button>';
                                }
                            })
                            .catch(err => {
                                document.getElementById('status').innerText = 'Status Unknown';
                            });
                    }
                    checkStatus();

                    function downloadModel() {
                        if(!confirm('Download 3MB ML Model? This requires internet access.')) return;

                        document.getElementById('status').innerHTML = 'Downloading model... please wait...';
                        fetch('/api/model/download', { method: 'POST' })
                            .then(response => {
                                if (response.ok) {
                                    alert('Model downloaded and loaded!');
                                    checkStatus();
                                } else {
                                    alert('Download failed.');
                                    checkStatus();
                                }
                            })
                            .catch(err => {
                                alert('Error: ' + err.message);
                                checkStatus();
                            });
                    }

                    function loadArticles() {
                         fetch('/api/articles')
                        .then(response => {
                            if (!response.ok) {
                                return response.text().then(text => { throw new Error(text || response.statusText) });
                            }
                            return response.json();
                        })
                        .then(data => {
                            const container = document.getElementById('articles');
                            if (data.length === 0) {
                                container.innerHTML = '<p>No articles found. Check backend logs.</p>';
                                return;
                            }
                            container.innerHTML = '';
                            data.forEach(article => {
                                const div = document.createElement('div');
                                div.className = 'article';
                                div.innerHTML = `
                                    <h2><a href="${'$'}{article.link}" target="_blank">${'$'}{article.title}</a></h2>
                                    <p>${'$'}{article.description}</p>
                                    <small>Score: ${'$'}{article.score}</small>
                                    <button onclick="likeArticle(${'$'}{article.id})">Like</button>
                                `;
                                container.appendChild(div);
                            });
                        })
                        .catch(err => {
                            document.getElementById('articles').innerHTML = '<p style="color:red">Error loading articles: ' + err.message + '</p>';
                        });
                    }

                    loadArticles();

                    function likeArticle(id) {
                        fetch(`/api/articles/${'$'}{id}/like`, { method: 'POST' })
                            .then(response => {
                                if (response.ok) alert('Article liked! Engine will learn.');
                                else alert('Failed to like.');
                            });
                    }

                    function refreshFeeds() {
                        fetch('/api/refresh', { method: 'POST' })
                            .then(response => {
                                if (response.ok) {
                                    alert('Refresh started. Please wait a moment.');
                                    setTimeout(loadArticles, 3000); // Reload after 3s
                                } else {
                                    alert('Failed to trigger refresh');
                                }
                            });
                    }
                </script>
            </body>
            </html>
        """.trimIndent()
    }
}
