# TDM-DataAPI
A simple service for retrieving TDM EF data for a given set of document IDs

# Build
`sbt clean stage`
Then find the result in `target/universal/stage/`

# Deploy
Copy the folder `target/universal/stage/` to the deployment location and rename as desired (henceforth referred to as `DEPLOY_DIR`).

# Setup
0. Generate an application secret by running `sbt playGenerateSecret`
1. Set `MONGODB_URI` environment variable to point to the Mongo instance holding the data
2. Set `DATAAPI_SECRET` environment variable to the value generated by step 0

(alternatively, these settings can also be configured by editing `target/universal/stage/conf/application.conf`)

# Run
*Note:* You must have the environment variables set before running (or edited the `application.conf` accordingly)
```bash
$DEPLOY_DIR/bin/tdm-dataapi -Dhttp.address=HOST -Dhttp.port=PORT -Dplay.http.context=/api
```
where `HOST` is the desired hostname or IP to bind to, and `PORT` is the desired port to run on.

# API

## Request format
### Single retrieval
```
GET   /api/v1/features/<urlencoded document id>
```
### Bulk retrieval
```
POST  /api/v1/features
      where the body is Content-Type: text/plain and contains:
      ID1|ID2|ID3|...
      or
      ID1
      ID2
      ID3
      ...
```
Note: For the POST request, the document IDs need not be urlencoded.

## Response format
The response follows the [jsonlines.org](jsonlines.org) format (i.e. one JSON object per line)

Note: The service respects the HTTP `Accept` header.
      The service can return responses in GZIP format by setting `Accept-Encoding: gzip` request header.

# Example

To retrieve document features for document with id `http://hdl.handle.net/2027/hvd.32044107317042`, use:  
`curl -v -X GET 'http://HOSTNAME:PORT/api/v1/features/http%3a%2f%2fhdl.handle.net%2f2027%2fhvd.32044107317042'`

Which returns data similar to the following snippet:
```
{"@context":"https://worksets.htrc.illinois.edu/context/ef_context.json","type":"Dataset","creator":{"id":"http://viaf.org/viaf/307472294","type":"Organization","name":"HathiTrust Research Center"},"schemaVersion":"4.0","dateCreated":"2018-08-30T05:27","features":{"pageCount":14,"pages":[{"seq":"00000001","version":"5a3ab3172703cf64faf50617d606a4db","language":"en","tokenCount":3,"lineCount":1,"emptyLineCount":0,"sentenceCount":1,"header":null,"body":{"tokenCount":3,"lineCount":1,"emptyLineCount":0,"sentenceCount":1,"capAlphaSeq":1,"beginCharCount":{"A":1},"endCharCount":{"r":1},"tokenPosCount":{"Actinometra":{"NNP":1},"Springer":{"NNP":1},"iowensisFrank":{"NNP":1}}},"footer":null}, ... }
```
