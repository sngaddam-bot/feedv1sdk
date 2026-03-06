# Table of contents

* [Features](#features)
* [Avro Deserialization](#avro-deserialization)
* [Usage](#usage)
* [Logging](#logging)
* [License](#license)
* [Contributing](#contributing)

# Features

The `FeedV1SDK` project is a Java-based application that interacts with eBay's Feed API. It uses Maven as a build and dependency management tool. The
project includes functionalities to download, unzip, and search through eBay's feed files.

Key functionalities include:

1. `downloadLatestFile`: This method downloads the latest file from the eBay feed. It takes parameters such as range value, queue size, feed type,
   category ID, marketplace ID, and the output filename for the zipped file.

2. `unzip`: This method unzips the downloaded file. It takes the zipped output filename and the unzipped output filename as parameters.

3. `findItem`: This method searches for a specific item in the unzipped feed file. It takes the item ID, unzipped output filename, and filtered output
   filename as parameters.

4.`AvroDeserializer`: A mini utility for deserializing Avro-formatted feed files with support for streaming large files, binary data, and JSON conversion.

Additionally, there's a `Contributing.md` file that provides guidelines for contributing to the project, including bug reporting, submitting changes,
and code style.

There are individual methods as well:

* CallGetFeedTypes : To get the list of feed types
* CallGetFiles : To get the list of files for a given feed type
* CallGetFile : To the file metadata
* CallGetAccess : To get the access configuration
* CallDownloadFile : To download the feed file

For more details on Feed V1 API, please refer to the [documentation](https://developer.ebay.com/api-docs/buy/feed/v1/static/overview.html).

# Setup

The SDK can be added as a maven dependency or the entire repository can be cloned/forked and changes can be made.

You are most welcome to collaborate and enhance the existing code base.

## Add as maven dependency

```
<!-- https://mvnrepository.com/artifact/com.ebay.api/feed-sdk -->
<dependency>
    <groupId>com.ebay.developer</groupId>
    <artifactId>feedv1-api-sdk</artifactId>
    <version>1.0.0-RELEASE</version>
</dependency>
```

## Setting up in the local environment

For setting up the project in your local environment

* Clone the repository
* Run mvn clean install.
  This should generate an uber jar with the following naming convention

__feed-sdk-{version}-uber.jar__

You might need to add the below in VM options
  --add-exports=java.base/sun.nio.ch=ALL-UNNAMED
  --add-opens=java.base/java.lang=ALL-UNNAMED
  --add-opens=java.base/java.lang.reflect=ALL-UNNAMED
  --add-opens=java.base/java.io=ALL-UNNAMED
  --add-opens=java.base/java.util=ALL-UNNAMED
  --add-opens
java.desktop/java.awt.font=ALL-UNNAMED
  --add-opens
java.base/java.text=ALL-UNNAMED
  --add-exports=jdk.unsupported/sun.misc=ALL-UNNAMED

## Downloading feed files

Since the feed API supports feed files which may be as big as 5 GB, there is a capability which was built into the system to download the file in
chunks of 100 MB.

The SDK abstracts the complexity involved in calculating the request header '__range__' based on the response header '__content-range__'.

To download a feed file of a particular cateogry and marketplace, the following code can be used -

```
CredentialUtil.load(new FileInputStream(path));
feedV1SDK.filterByItem(rangeValue, queueSize, "CURATED_ITEM_FEED", categoryId, marketplaceId, "1234567890", zippedOutputFilename, unzippedOutputFilename, filteredOutputFilename);

```

The __GetFeedResponse.filePath__ denotes the location where the file was downloaded.

### Customizing download location

The default download location is the current working directory.
The download location can be changed by specifying the optional 'downloadDirectory' parameter in the feed method.
For example, to download to the location __/tmp/feed__ -

```
feed.get(builder.build(), "/tmp/feed");
```

---

## Unzipping feed files

Since the feed file is gzipped, it needs to be unzipped before post processing tasks can be run on it.

To unzip a downloaded feed file -

```
Feed feed = new FeedImpl();
String unzippedFilePath = feed.unzip(filePath)

```

---

## Logging

Logging is configured using slf4j and rolling log files are created in the current directory.

---

## Usage

The following sections describe the different ways in which the SDK can be used

#### Examples

All the examples are located [__here__](https://github.com/eBay/feedv1sdk/tree/main/feedv1-api-sdk-examples/src/main/java/com/ebay/feed/examples)

* [Filter by item](https://github.com/ranallurebay/feedv1sdk/blob/main/feedv1-api-sdk-examples/src/main/java/com/ebay/feed/examples/FeedV1SDKUsage.java)

## Important notes

* Ensure there is enough storage for unzipped files
* Ensure that the log and file storage directories have appropriate write permissions
* In case of failure in downloading due to network issues, the process needs to start again. There is no capability at the moment, to resume.

## License

Copyright 2024 eBay Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

<https://www.apache.org/licenses/LICENSE-2.0>

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
